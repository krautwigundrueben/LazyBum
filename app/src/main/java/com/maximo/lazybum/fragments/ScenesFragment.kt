package com.maximo.lazybum.fragments

import android.content.Context
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.ApiRequest
import com.maximo.lazybum.Globals
import com.maximo.lazybum.R
import com.maximo.lazybum.arduinoApi.Arduino
import com.maximo.lazybum.commands.Cmd
import com.maximo.lazybum.commands.LedGridCmd
import com.maximo.lazybum.commands.SpotsCmd
import com.maximo.lazybum.myStromApi.D8E3D9494
import com.maximo.lazybum.shellyApi.ShellyLight
import com.maximo.lazybum.uiComponents.*
import kotlinx.android.synthetic.main.list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val baseUrl = "http://192.168.178."

class ScenesFragment : Fragment() {

    companion object{

        val sectionHeaderList = mutableListOf<ListRow>(
            ListSectionHeader(id = 0, text = "Bewegtbild"),
            ListSectionHeader(id = 3, text = "Auf die Ohren"),
            ListSectionHeader(id = 6, text = "Licht und Schatten"))
        val sceneList = mutableListOf(
            ListScene(id = 1, text = "Fernsehen", img = R.drawable.ic_monitor, description = "LED Grid gedimmt - Spots aus - TV an",
                actionList = mutableListOf(
                    DevicesFragment.deviceList.find{it.action.deviceId == 32.toLong()}?.action!!.copy(cmd = LedGridCmd("on", "33000000", "rgb", 2000)),
                    DevicesFragment.deviceList.find{it.action.deviceId == 45.toLong()}?.action!!.copy(cmd = SpotsCmd("off", "40")),
                    DevicesFragment.deviceList.find{it.action.deviceId == 43.toLong()}?.action!!.copy(cmd = Cmd("on")),
                    DevicesFragment.deviceList.find{it.action.deviceId == 99.toLong()}?.action!!.copy(),
                    (AvReceiverFragment.deviceList.find{it.id == 0.toLong()} as ListAction).action.copy()
                )),
            ListScene(id = 2, text = "Großleinwand", img = R.drawable.ic_football, description = "LED Grid aus - Spots aus - Receiver an",
                actionList = mutableListOf(
                    DevicesFragment.deviceList.find{it.action.deviceId == 32.toLong()}?.action!!.copy(cmd = LedGridCmd("off", "33000000", "rgb", 2000)),
                    DevicesFragment.deviceList.find{it.action.deviceId == 45.toLong()}?.action!!.copy(cmd = SpotsCmd("off", "40")),
                    DevicesFragment.deviceList.find{it.action.deviceId == 99.toLong()}?.action!!.copy(),
                    (AvReceiverFragment.deviceList.find{it.id == 0.toLong()} as ListAction).action.copy()
                )),
            ListScene(id = 4, text = "Spotify", img = R.drawable.ic_music, description = "Quelle Bose Adapter",
                actionList = mutableListOf(
                    (AvReceiverFragment.deviceList.find{it.id == 1.toLong()} as ListAction).action.copy()
                )),
            ListScene(id = 5, text = "Bose Soundtouch", img = R.drawable.ic_music, description = "Quelle Bose Adapter",
                actionList = mutableListOf(
                    (AvReceiverFragment.deviceList.find{it.id == 1.toLong()} as ListAction).action.copy()
                )),
            ListScene(id = 7, text = "Soirée", img = R.drawable.ic_dining, description = "gedimmtes Licht im Wohnzimmer",
                actionList = mutableListOf(
                    DevicesFragment.deviceList.find{it.action.deviceId == 32.toLong()}?.action!!.copy(cmd = LedGridCmd("on", "99000000", "rgb", 2000)),
                    DevicesFragment.deviceList.find{it.action.deviceId == 45.toLong()}?.action!!.copy(cmd = SpotsCmd("on", "20")),
                )),
            ListScene(id = 8, text = "Heißer Tag", img = R.drawable.ic_shutter, description = "alle Rollos zu",
                actionList = mutableListOf(
                    RollerFragment.deviceList.find { it.action.deviceId == 51.toLong() }?.action!!.copy(nextCmd = "close"),
                    RollerFragment.deviceList.find { it.action.deviceId == 54.toLong() }?.action!!.copy(nextCmd = "close"),
                    RollerFragment.deviceList.find { it.action.deviceId == 55.toLong() }?.action!!.copy(nextCmd = "close"),
                    RollerFragment.deviceList.find { it.action.deviceId == 56.toLong() }?.action!!.copy(nextCmd = "close"),
                    RollerFragment.deviceList.find { it.action.deviceId == 57.toLong() }?.action!!.copy(nextCmd = "close"),
                ))
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val btnListView: MutableList<ListRow> = sceneList.union(sectionHeaderList).sortedBy { it.id }.toMutableList()

        val view = inflater.inflate(R.layout.list, container, false)
        val listView = view.list
        listView.adapter = MyListAdapter(requireContext(), btnListView)
        listView.setOnItemClickListener { parent, v, position, id ->
            val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!btnListView[position].isHeader) {
                if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                    val scene = btnListView[position] as ListScene
                    execute(scene.actionList, listView)
                } else {
                    Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun execute(actionList: MutableList<Action>, listView: ListView) {

        for (action in actionList) {

            val client = OkHttpClient().newBuilder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(3, TimeUnit.SECONDS)
                .build()

            val api = Retrofit.Builder()
                .baseUrl(action.url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(ApiRequest::class.java)

            GlobalScope.launch(Dispatchers.IO) {

                val response: Response<JsonObject>

                try {
                    when (action.deviceId.toInt()) {
                        43 -> {
                            val r: Response<Void>
                            r = api.switch(1).awaitResponse()
                            if (r.isSuccessful) {
                                DevicesFragment.deviceList.find{it.action.deviceId == 43.toLong()}?.isOn = true
                            }
                        }
                        32 -> {
                            response =
                                api.set((action.cmd as LedGridCmd).color,
                                    (action.cmd as LedGridCmd).mode,
                                    action.cmd.action,
                                    (action.cmd as LedGridCmd).ramp)
                                    .awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                val dataJson = data?.getAsJsonObject("840D8E3D9494")
                                val deviceData = Gson().fromJson<D8E3D9494>(dataJson, D8E3D9494::class.java)
                                DevicesFragment.deviceList.find{it.action.deviceId == 32.toLong()}?.apply { isOn = true }

                                val rgb = deviceData.color.substring(2, 8)
                                val ww = deviceData.color.substring(0, 2)
                                if (DevicesFragment.deviceList.find{it.action.deviceId == 32.toLong()}?.isOn!!) {
                                    if (rgb != "000000") {
                                        DevicesFragment.gridColor = Color.parseColor("#" + rgb)
                                    } else {
                                        DevicesFragment.gridColor = Color.parseColor("#" + ww + ww + ww)
                                    }
                                }
                            }
                        }
                        45 -> {
                            var mSpotBrightness: Int = 0
                            action.cmd as SpotsCmd
                            response = api.set(action.cmd.action, (action.cmd as SpotsCmd).value).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                val light = Gson().fromJson(data,
                                    ShellyLight::class.java)
                                DevicesFragment.deviceList.find{it.action.deviceId == 45.toLong()}?.apply { isOn = true }
                                mSpotBrightness = light.brightness
                            }
                            if (DevicesFragment.deviceList.find{it.action.deviceId == 45.toLong()}?.isOn!!)
                                DevicesFragment.spotBrightness = mSpotBrightness
                            else DevicesFragment.spotBrightness = 0
                        }
                        99 -> {
                            response = api.sendCommand(action.cmd.action).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                AvReceiverFragment.arduino.AvRec = Gson().fromJson(data, Arduino::class.java).AvRec
                                AvReceiverFragment.arduino.SkyRec = Gson().fromJson(data, Arduino::class.java).SkyRec

                                for (device in AvReceiverFragment.deviceList) {
                                    (device as ListAction).isOn = false
                                }

                                if (AvReceiverFragment.arduino.AvRec.isOn) {
                                    when (AvReceiverFragment.arduino.AvRec.mode) {
                                        // AvRec Modes: 1 = Sky, 2 = Chromecast, 4 = Bose
                                        1 -> (AvReceiverFragment.deviceList[0] as ListAction).isOn = true
                                        2 -> (AvReceiverFragment.deviceList[2] as ListAction).isOn = true
                                        4 -> (AvReceiverFragment.deviceList[1] as ListAction).isOn = true
                                    }
                                }

                                DevicesFragment.deviceList.find { it.id == 8.toLong() }?.isOn = AvReceiverFragment.arduino.SkyRec.isOn
                            }
                        }
                        else -> {
                        }
                    }
                    withContext(Dispatchers.Main) {
                        (listView.adapter as MyListAdapter).notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}