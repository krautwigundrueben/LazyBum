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

class RollerFragment : Fragment() {

    val deviceList = mutableListOf<Device>(shutter)
    var nextGo = "close"
    val supportedWifiSsids = immutableListOf<String>("\"DasWeltweiteInternetz\"", "\"AndroidWifi\"")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {



        val sectionHeaderList = immutableListOf<ListItem>(
            SectionHeader(title = "Kinderzimmer"))

        val btnListView = mutableListOf<ListItem>()
        val headerPositions = intArrayOf(0)
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
                        6 -> {
                            if (command.action == "getStatus") {
                                response = api.getShutterStatus().awaitResponse()
                            } else {
                                response = api.go(nextGo).awaitResponse()
                            }
                            if (response.isSuccessful) {
                                val data = response.body()
                                val shutter = Gson().fromJson(data, ShellyShutter::class.java)

                                if (shutter.state == "stop") {
                                    if (shutter.last_direction == "close") {
                                        nextGo = "open"
                                    } else {
                                        nextGo = "close"
                                    }
                                } else {
                                    nextGo = "stop"
                                }
                            }
                        }
                        else -> {
                        }
                    }

                    withContext(Dispatchers.Main) {

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

        val shutter = Device(6, "linke Seite", R.drawable.ic_shutter,
            baseUrl + "51", false, Command("wechselnd runter | stop | hoch", "", "", "", "", 0))
    }
}