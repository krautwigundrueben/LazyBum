package com.maximo.lazybum.fragments

import android.app.AlertDialog
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
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jem.rubberpicker.RubberSeekBar
import com.maximo.lazybum.ApiRequest
import com.maximo.lazybum.Globals
import com.maximo.lazybum.R
import com.maximo.lazybum.arduinoApi.Arduino
import com.maximo.lazybum.commands.Cmd
import com.maximo.lazybum.commands.CmdInterface
import com.maximo.lazybum.commands.LedGridCmd
import com.maximo.lazybum.commands.SpotsCmd
import com.maximo.lazybum.myStromApi.D8E3D9494
import com.maximo.lazybum.myStromApi.Relay
import com.maximo.lazybum.shellyApi.ShellyLight
import com.maximo.lazybum.shellyApi.ShellyLightsStatus
import com.maximo.lazybum.shellyApi.ShellyRelay
import com.maximo.lazybum.shellyApi.ShellyRelayStatus
import com.maximo.lazybum.uiComponents.*
import kotlinx.android.synthetic.main.brightness_dialog.view.*
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

class DevicesFragment : Fragment() {

    companion object{
        val sectionHeaderList = mutableListOf(
            ListSectionHeader(id = 0, text = "Küche"),
            ListSectionHeader(id = 2, text = "Essbereich"),
            ListSectionHeader(id = 4, text = "Wohnzimmer"))
        val deviceList = mutableListOf(
            ListAction(id = 1, text = "Kaffeemaschine", img = R.drawable.ic_coffee, description = "wechselnd an | aus",
                action = Action(deviceId = 47, url = baseUrl + "47", cmd = Cmd(""))),
            ListAction(id = 3, text = "Esstischlampe", img = R.drawable.ic_dining, description = "wechselnd an | aus",
                action = Action(deviceId = 46, url = baseUrl + "46", cmd = Cmd("toggle"))),
            ListAction(id = 5, text = "LED Grid", img = R.drawable.ic_led_grid, description = "wechselnd an | aus - lang drücken für mehr",
                action = Action(deviceId = 32, url = baseUrl + "32", cmd = LedGridCmd("toggle", "33000000", "rgb", 2000))),
            ListAction(id = 6, text = "Strahler", img = R.drawable.ic_spots, description = "wechselnd an | aus - lang drücken für mehr",
                action = Action(deviceId = 45, url = baseUrl + "45", cmd = SpotsCmd("toggle", "40"))),
            ListAction(id = 7, text = "Fernseher", img = R.drawable.ic_monitor, description = "wechselnd an | aus",
                action = Action(deviceId = 43, url = baseUrl + "43", cmd = Cmd(""))),
            ListAction(id = 8, text = "TV Receiver", img = R.drawable.ic_sky, description = "wechselnd an | aus",
                action = Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("toggleSky"))))
        var gridColor = 0xff0000ff.toInt()
        var spotBrightness = 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager

        val view = inflater.inflate(R.layout.list, container, false)
        val listView = view.list
        val btnListView: MutableList<ListRow> = deviceList.union(sectionHeaderList).sortedBy { it.id }.toMutableList()
        listView.adapter = MyListAdapter(requireContext(), btnListView)
        listView.setOnItemClickListener { parent, v, position, id ->
            if (!btnListView[position].isHeader) {
                if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                    val clickedDevice = btnListView[position] as ListAction
                    execute(clickedDevice, clickedDevice.action.cmd, listView)
                } else {
                    Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
                }
            }
        }

        listView.setOnItemLongClickListener { parent, v, position, id ->
            if (!btnListView[position].isHeader) {
                val clickedDevice = btnListView[position] as ListAction
                when (clickedDevice.id.toInt()) {
                    5 -> ColorPickerDialogBuilder
                        .with(context)
                        .setTitle("Farbe wählen")
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .initialColor(gridColor)
                        .density(6)
                        .lightnessSliderOnly()
                        .setOnColorChangedListener { selectedColor ->
                            if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                                var color = "00" + Integer.toHexString(selectedColor).takeLast(6)
                                if (color.regionMatches(2, color, 4, 2) && color.regionMatches(2, color, 6, 2))
                                    color = color.substring(2, 4) + "000000"
                                execute(btnListView[position] as ListAction,
                                    LedGridCmd("on", color, "rgb", 0),
                                    listView)
                            }
                            else {
                                Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setPositiveButton("Ok") { dialog, selectedColor, allColors ->
                            gridColor = selectedColor
                        }
                        .build()
                        .show()
                    6 -> {
                        val mDialogView =
                            LayoutInflater.from(activity).inflate(R.layout.brightness_dialog, null)
                        val rubberSeekBar = mDialogView.rubberSeekBar

                        AlertDialog.Builder(activity)
                            .setView(mDialogView)
                            .setTitle("Helligkeit wählen")
                            .setPositiveButton("Ok") { diaglog, selectedBrightness -> }
                            .show()

                        rubberSeekBar.setCurrentValue(spotBrightness)
                        rubberSeekBar.setOnRubberSeekBarChangeListener(object :
                            RubberSeekBar.OnRubberSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: RubberSeekBar,
                                value: Int,
                                fromUser: Boolean
                            ) {
                                spotBrightness = value
                            }

                            override fun onStartTrackingTouch(seekBar: RubberSeekBar) {}

                            override fun onStopTrackingTouch(seekBar: RubberSeekBar) {
                                if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                                    execute(btnListView[position] as ListAction,
                                        SpotsCmd("on", spotBrightness.toString()),
                                        listView)
                                }
                                else {
                                    Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    }
                }
            }

            true
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
            val listView = requireView().list
            for (device in deviceList) {
                execute(device, Cmd("getStatus"), listView)
            }
        } else {
            Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
        }
    }

    private fun execute(listAction: ListAction, cmd: CmdInterface, listView: ListView) {
        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(3, TimeUnit.SECONDS)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(listAction.action.url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO) {

            val response: Response<JsonObject>

            try {
                when (listAction.id.toInt()) {
                    1, 7 -> {
                        if (cmd.action == "getStatus") {
                            response = api.getReport().awaitResponse()
                        } else {
                            response = api.toggle().awaitResponse()
                        }
                        if (response.isSuccessful) {
                            val data = response.body()
                            listAction.isOn = Gson().fromJson(data, Relay::class.java).relay
                        }
                    }
                    5 -> {
                        if (cmd.action == "getStatus") {
                            response = api.getInfo().awaitResponse()
                        } else {
                            cmd as LedGridCmd
                            response =
                                api.set(cmd.color,
                                    cmd.mode,
                                    cmd.action,
                                    cmd.ramp)
                                    .awaitResponse()
                        }
                        if (response.isSuccessful) {
                            val data = response.body()
                            val dataJson = data?.getAsJsonObject("840D8E3D9494")
                            val deviceData = Gson().fromJson<D8E3D9494>(dataJson, D8E3D9494::class.java)
                            listAction.isOn = deviceData.on

                            val rgb = deviceData.color.substring(2, 8)
                            val ww = deviceData.color.substring(0, 2)
                            if (listAction.isOn) {
                                if (rgb != "000000") {
                                    gridColor = Color.parseColor("#" + rgb)
                                } else {
                                    gridColor = Color.parseColor("#" + ww + ww + ww)
                                }
                            }
                        }
                    }
                    6 -> {
                        var mSpotBrightness: Int = 0
                        if (cmd.action == "getStatus") {
                            response = api.getStatus().awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                val light = Gson().fromJson(data,
                                    ShellyLightsStatus::class.java).lights[0]
                                listAction.isOn = light.ison
                                mSpotBrightness = light.brightness
                            }
                        } else {
                            cmd as SpotsCmd
                            response = api.set(cmd.action, cmd.value).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                val light = Gson().fromJson(data,
                                    ShellyLight::class.java)
                                listAction.isOn = light.ison
                                mSpotBrightness = light.brightness
                            }
                        }
                        if (listAction.isOn) spotBrightness = mSpotBrightness
                        else spotBrightness = 0
                    }
                    3 -> {
                        if (cmd.action == "getStatus") {
                            response = api.getStatus().awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                listAction.isOn = Gson().fromJson(data,
                                    ShellyRelayStatus::class.java).relays[0].ison
                            }
                        } else {
                            response = api.set(cmd.action).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                listAction.isOn =
                                    Gson().fromJson(data, ShellyRelay::class.java).ison
                            }
                        }
                    }
                    8 -> {
                        response = api.sendCommand(cmd.action).awaitResponse()
                        if (response.isSuccessful) {
                            val data = response.body()
                            listAction.isOn = Gson().fromJson(data, Arduino::class.java).SkyRec.isOn
                        }
                    }
                    else -> { }
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