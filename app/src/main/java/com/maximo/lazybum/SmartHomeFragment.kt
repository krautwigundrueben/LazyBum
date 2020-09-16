package com.maximo.lazybum

import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.Devices.arduinoApi.Arduino
import com.maximo.lazybum.Devices.arduinoApi.Command
import com.maximo.lazybum.Devices.arduinoApi.Device
import com.maximo.lazybum.myStromApi.D8E3D9494
import com.maximo.lazybum.myStromApi.Relay
import com.maximo.lazybum.shellyApi.ShellyLight
import com.maximo.lazybum.shellyApi.ShellyLightsStatus
import com.maximo.lazybum.shellyApi.ShellyRelay
import com.maximo.lazybum.shellyApi.ShellyRelayStatus
import kotlinx.android.synthetic.main.fragment_smart_home.view.*
import kotlinx.android.synthetic.main.row.view.*
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

private const val baseUrl = "http://192.168.178."

class SmartHomeFragment : Fragment() {

    val TAG = "SmartHomeFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val deviceList = mutableListOf<Device>()

        deviceList.addAll(listOf(coffee, grid, spots, diningLight, tv, skyReceiver))

        val view = inflater.inflate(R.layout.fragment_smart_home, container, false)
        val listView = view.smart_home_list
        listView.adapter = MyListAdapter(requireContext(), R.layout.row, deviceList)

        listView.setOnItemClickListener { parent, v, position, id ->
            execute(deviceList[position], deviceList[position].command, listView)
        }

        for (device in deviceList) {
            execute(device, Command("getStatus", "", "", "", 0), listView)
        }

        return view
    }

    private fun execute(device: Device, command: Command, listView: ListView) {

        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
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
                    0, 4 -> {
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
                    1 -> {
                        if (command.action == "getStatus") {
                            response = api.getInfo().awaitResponse()
                        } else {
                            response =
                                api.set(command.color, command.mode, command.action, command.ramp)
                                    .awaitResponse()
                        }
                        if (response.isSuccessful) {
                            val data = response.body()
                            val deviceObject = data?.getAsJsonObject("840D8E3D9494")
                            device.isOn = Gson().fromJson<D8E3D9494>(deviceObject, D8E3D9494::class.java).on
                        }
                    }
                    2 -> {
                        if (command.action == "getStatus") {
                            response = api.getStatus().awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data,ShellyLightsStatus::class.java).lights[0].ison
                            }
                        } else {
                            response = api.set(command.action, command.value).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, ShellyLight::class.java).ison
                            }
                        }
                    }
                    3 -> {
                        if (command.action == "getStatus") {
                            response = api.getStatus().awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data,ShellyRelayStatus::class.java).relays[0].ison
                            }
                        } else {
                            response = api.set(command.action).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, ShellyRelay::class.java).ison
                            }
                        }
                    }
                    5 -> {
                        response = api.sendCommand(command.action).awaitResponse()
                        if (response.isSuccessful) {
                            val data = response.body()
                            device.isOn = Gson().fromJson(data, Arduino::class.java).SkyRec.isOn
                        }
                    }
                    else -> { }
                }

                withContext(Dispatchers.Main) {
                    if (device.isOn) {
                        listView.getChildAt(device.id).imageView.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                    } else {
                        listView.getChildAt(device.id).imageView.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.colorOff), PorterDuff.Mode.SRC_IN)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object{
        val coffee = Device(0,"Kaffeemaschine","Küche", R.drawable.ic_coffee,
            baseUrl + "47",false, Command("", "", "", "", 0))

        val grid = Device(1,"LED Grid","Wohnzimmer", R.drawable.ic_led_grid,
            baseUrl + "32",false, Command("toggle", "", "33000000", "rgb", 2000))

        val spots = Device(2,"Strahler","Wohnzimmer", R.drawable.ic_spots,
            baseUrl + "45",false, Command("toggle", "40", "", "", 0))

        val diningLight = Device(3,"Esstischlampe","Essbereich", R.drawable.ic_dining,
            baseUrl + "46",false, Command("toggle", "", "", "", 0))

        val tv = Device(4,"Fernseher", "Wohnzimmer", R.drawable.ic_monitor,
            baseUrl + "43", false, Command("", "", "", "", 0))

        val skyReceiver = Device(5, "Sky Receiver", "Wohnzimmer", R.drawable.ic_sky,
            arduinoBaseUrl, false, Command("toggleSky", "", "", "", 0))
    }
}