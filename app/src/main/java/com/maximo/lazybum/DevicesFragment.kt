package com.maximo.lazybum

import android.app.AlertDialog
import android.content.Context
import android.graphics.PorterDuff
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jem.rubberpicker.RubberSeekBar
import com.maximo.lazybum.Devices.arduinoApi.Arduino
import com.maximo.lazybum.Devices.arduinoApi.Command
import com.maximo.lazybum.Devices.arduinoApi.Device
import com.maximo.lazybum.myStromApi.D8E3D9494
import com.maximo.lazybum.myStromApi.Relay
import com.maximo.lazybum.shellyApi.*
import kotlinx.android.synthetic.main.brightness_dialog.view.*
import kotlinx.android.synthetic.main.fragment_smart_home.view.*
import kotlinx.android.synthetic.main.row.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.internal.immutableListOf
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


private const val baseUrl = "http://192.168.178."

class DevicesFragment : Fragment() {

    val deviceList = mutableListOf<Device>(coffee, diningLight, grid, spots, tv, skyReceiver)
    var gridColor = 0xff0000ff.toInt()
    var spotBrightness = 0
    val supportedWifiSsids = immutableListOf<String>("\"DasWeltweiteInternetz\"", "\"AndroidWifi\"")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val sectionHeaderList = immutableListOf<ListItem>(
            SectionHeader(title = "Küche"),
            SectionHeader(title = "Essbereich"),
            SectionHeader(title = "Wohnzimmer"))

        val btnListView = mutableListOf<ListItem>()
        val headerPositions = intArrayOf(0, 2, 4)
        var h = 0
        var d = 0
        val lastListItem = deviceList.size + sectionHeaderList.size - 1
        for (i in 0..lastListItem)
        {
            if (i in headerPositions) {
                btnListView.add(sectionHeaderList[h])
                h++
            }
            else {
                btnListView.add(deviceList[d])
                d++
            }
        }

        val view = inflater.inflate(R.layout.fragment_smart_home, container, false)
        val listView = view.smart_home_list
        listView.adapter = MyListAdapter(requireContext(), btnListView)

        listView.setOnItemClickListener { parent, v, position, id ->
            if (position !in headerPositions) {
                val clickedDevice = btnListView[position] as Device
                execute(clickedDevice, clickedDevice.command, listView)
            }
        }

        listView.setOnItemLongClickListener { parent, v, position, id ->
            if (position !in headerPositions) {
                val clickedDevice = btnListView[position] as Device
                when (clickedDevice.id) {
                    5 -> ColorPickerDialogBuilder
                        .with(context)
                        .setTitle("Farbe wählen")
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .initialColor(gridColor)
                        .density(12)
                        .lightnessSliderOnly()
                        .setOnColorSelectedListener { selectedColor ->
                            val color = "00" + Integer.toHexString(selectedColor).takeLast(6)
                            execute(deviceList[position],
                                Command("", "on", "", color, "rgb", 0),
                                listView)
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
                                execute(deviceList[position],
                                    Command("", "on", spotBrightness.toString(), "", "", 0),
                                    listView)
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


        val listView = requireView().smart_home_list
        /*
        if (deviceList.isEmpty()) {
            deviceList.addAll(listOf(coffee, grid, spots, diningLight, tv, skyReceiver))
        }
        */
        // get status and set status colors anew
        val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (connMgr.connectionInfo.ssid == supportedWifiSsids[0] || connMgr.connectionInfo.ssid == supportedWifiSsids[1]) {
            for (device in deviceList) {
                execute(device, Command("", "getStatus", "", "", "", 0), listView)
            }
        } else {
            Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
        }
    }

    private fun execute(device: Device, command: Command, listView: ListView) {

        val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (connMgr.connectionInfo.ssid == supportedWifiSsids[0] || connMgr.connectionInfo.ssid == supportedWifiSsids[1]) {

            val client = OkHttpClient().newBuilder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(3, TimeUnit.SECONDS)
                .build()

            val api = Retrofit.Builder()
                .baseUrl(device.url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(ApiRequest::class.java)

            GlobalScope.launch(Dispatchers.IO) {

                val response: Response<JsonObject>

                try {
                    when (device.id) {
                        1, 7 -> {
                            if (command.action == "getStatus") {
                                response = api.getReport().awaitResponse()
                            } else {
                                response = api.toggle().awaitResponse()
                            }
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, Relay::class.java).relay
                            }
                        }
                        5 -> {
                            if (command.action == "getStatus") {
                                response = api.getInfo().awaitResponse()
                            } else {
                                response =
                                    api.set(command.color,
                                        command.mode,
                                        command.action,
                                        command.ramp)
                                        .awaitResponse()
                            }
                            if (response.isSuccessful) {
                                val data = response.body()
                                val deviceObject = data?.getAsJsonObject("840D8E3D9494")
                                device.isOn =
                                    Gson().fromJson<D8E3D9494>(deviceObject,
                                        D8E3D9494::class.java).on
                            }
                        }
                        6 -> {
                            var mSpotBrightness: Int = 0
                            if (command.action == "getStatus") {
                                response = api.getStatus().awaitResponse()
                                if (response.isSuccessful) {
                                    val data = response.body()
                                    val light = Gson().fromJson(data,
                                        ShellyLightsStatus::class.java).lights[0]
                                    device.isOn = light.ison
                                    mSpotBrightness = light.brightness
                                }
                            } else {
                                response = api.set(command.action, command.value).awaitResponse()
                                if (response.isSuccessful) {
                                    val data = response.body()
                                    val light = Gson().fromJson(data,
                                        ShellyLight::class.java)
                                    device.isOn = light.ison
                                    mSpotBrightness = light.brightness
                                }
                            }
                            if (device.isOn) spotBrightness = mSpotBrightness
                            else spotBrightness = 0
                        }
                        3 -> {
                            if (command.action == "getStatus") {
                                response = api.getStatus().awaitResponse()
                                if (response.isSuccessful) {
                                    val data = response.body()
                                    device.isOn = Gson().fromJson(data,
                                        ShellyRelayStatus::class.java).relays[0].ison
                                }
                            } else {
                                response = api.set(command.action).awaitResponse()
                                if (response.isSuccessful) {
                                    val data = response.body()
                                    device.isOn =
                                        Gson().fromJson(data, ShellyRelay::class.java).ison
                                }
                            }
                        }
                        8 -> {
                            response = api.sendCommand(command.action).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, Arduino::class.java).SkyRec.isOn
                            }
                        }
                        else -> {
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (device.isOn) {
                            listView.getChildAt(device.id).imageView.setColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.colorAccent),
                                PorterDuff.Mode.SRC_IN)
                        } else {
                            listView.getChildAt(device.id).imageView.setColorFilter(
                                ContextCompat.getColor(requireContext(), R.color.colorOff),
                                PorterDuff.Mode.SRC_IN)
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
        }
    }

    companion object{
        val coffee = Device(1, "Kaffeemaschine", R.drawable.ic_coffee,
            baseUrl + "47", false, Command("wechselnd an | aus","", "", "", "", 0))

        val grid = Device(5, "LED Grid", R.drawable.ic_led_grid,
            baseUrl + "32", false, Command("wechselnd an | aus - lang drücken für mehr", "toggle", "", "33000000", "rgb", 2000))

        val spots = Device(6, "Strahler", R.drawable.ic_spots,
            baseUrl + "45", false, Command("wechselnd an | aus - lang drücken für mehr","toggle", "40", "", "", 0))

        val diningLight = Device(3, "Esstischlampe",  R.drawable.ic_dining,
            baseUrl + "46", false, Command("wechselnd an | aus", "toggle", "", "", "", 0))

        val tv = Device(7, "Fernseher", R.drawable.ic_monitor,
            baseUrl + "43", false, Command("wechselnd an | aus", "", "", "", "", 0))

        val skyReceiver = Device(8, "TV Receiver",  R.drawable.ic_sky,
            arduinoBaseUrl, false, Command("wechselnd an | aus", "toggleSky", "", "", "", 0))
    }
}