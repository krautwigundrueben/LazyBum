package com.maximo.lazybum.fragments

import android.content.Context
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
                    Action(deviceId = 32, url = baseUrl + "32", cmd = LedGridCmd("on", "33000000", "rgb", 2000)),
                    Action(deviceId = 45, url = baseUrl + "45", cmd = SpotsCmd("off", "40")),
                    Action(deviceId = 43, url = baseUrl + "43", cmd = Cmd("on")),
                    Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("toggleSky")), //implement Arduino explicit on with TV request
                    Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("TV"))
                )),
            ListScene(id = 2, text = "Großleinwand", img = R.drawable.ic_football, description = "LED Grid aus - Spots aus - Receiver an",
                actionList = mutableListOf(
                    Action(deviceId = 32, url = baseUrl + "32", cmd = LedGridCmd("off", "33000000", "rgb", 2000)),
                    Action(deviceId = 45, url = baseUrl + "45", cmd = SpotsCmd("off", "40")),
                    Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("toggleSky")), //implement Arduino explicit on with TV request
                    Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("TV"))
                )),
            ListScene(id = 4, text = "Spotify", img = R.drawable.ic_music, description = "work in progress",
                actionList = mutableListOf(

                )),
            ListScene(id = 5, text = "Bose Soundtouch", img = R.drawable.ic_music, description = "work in progress",
                actionList = mutableListOf(

                )),
            ListScene(id = 7, text = "Soirée", img = R.drawable.ic_dining, description = "",
                actionList = mutableListOf(
                    Action(deviceId = 32, url = baseUrl + "32", cmd = LedGridCmd("on", "99000000", "rgb", 2000)),
                    Action(deviceId = 45, url = baseUrl + "45", cmd = SpotsCmd("on", "20")),
                )),
            ListScene(id = 8, text = "Heißer Tag", img = R.drawable.ic_shutter, description = "work in progress",
                actionList = mutableListOf(

                )))
        var nextGo = "close"
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
                                //val data = response.body()
                                //listAction.isOn = Gson().fromJson(data, Relay::class.java).relay
                            }
                        }
                        32 -> {
                            action.cmd as LedGridCmd
                            response =
                                api.set(action.cmd.color,
                                    action.cmd.mode,
                                    action.cmd.action,
                                    action.cmd.ramp)
                                    .awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                val dataJson = data?.getAsJsonObject("840D8E3D9494")
                                val deviceData =
                                    Gson().fromJson<D8E3D9494>(dataJson, D8E3D9494::class.java)
                            }
                        }
                        45 -> {
                            var mSpotBrightness: Int = 0
                            action.cmd as SpotsCmd
                            response = api.set(action.cmd.action, action.cmd.value).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                val light = Gson().fromJson(data,
                                    ShellyLight::class.java)
                                mSpotBrightness = light.brightness
                            }
                        }
                        99 -> {
                            response = api.sendCommand(action.cmd.action).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                            }
                        }
                        else -> {
                        }
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