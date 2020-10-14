package com.maximo.lazybum.fragments

import android.content.Context
import android.content.SharedPreferences
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
import com.maximo.lazybum.arduinoApi.AvRec
import com.maximo.lazybum.arduinoApi.SkyRec
import com.maximo.lazybum.commands.Cmd
import com.maximo.lazybum.uiComponents.Action
import com.maximo.lazybum.uiComponents.ListAction
import com.maximo.lazybum.uiComponents.ListRow
import com.maximo.lazybum.uiComponents.MyListAdapter
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener
import kotlinx.android.synthetic.main.fragment_media.view.*
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

const val arduinoBaseUrl = "http://192.168.178.108"

class AvReceiverFragment : Fragment() {

    companion object {

        val deviceList = mutableListOf<ListRow>(
            ListAction(id = 0, text = "Fernsehen", img = R.drawable.ic_football, description = "Quelle Sky Receiver",
                action = Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("TV"))),
            ListAction(id = 1, text = "Musik über Bose System", img = R.drawable.ic_music, description = "Quelle Bose Adapter",
                action = Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("Bose"))),
            ListAction(id = 2, text = "Musik oder Video über Chromecast", img = R.drawable.ic_film, description = "Quelle Chromecast",
                action = Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("CCaudio"))),
            ListAction(id = 3, text = "Ausschalten", img = R.drawable.ic_power_off, description = "schaltet AV Receiver aus",
                action = Action(deviceId = 99, url = arduinoBaseUrl, cmd = Cmd("DvcsOff")))
        )
        val arduino = Arduino(AvRec(false, 1, "aus"), SkyRec(false))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager

        val view = inflater.inflate(R.layout.fragment_media, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(), deviceList)
        listView.setOnItemClickListener { parent, v, position, id ->
            if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                val clickedDevice = deviceList[position] as ListAction
                execute(clickedDevice.action.cmd.action, listView)
            }
            else {
                Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
            }
        }

        val croller: Croller = view.croller

        val obj = object : OnCrollerChangeListener {
            override fun onProgressChanged(croller: Croller?, progress: Int) {
                if (arduino.AvRec.isOn) {
                    view!!.textView.text = (progress + 40).toString()
                }
            }

            override fun onStartTrackingTouch(croller: Croller?) { }

            override fun onStopTrackingTouch(croller: Croller?) {
                /*
                volume of AV Receiver has to be calculated according the boundary values:
                value in dB | description   | receiver annotation   | progress of seekbar
                -60dB       | silent        | 040VL                 | 0
                0dB         | loud!         | 160VL                 | 120
                */
                val seekbarProgress = croller?.progress!!
                val volInRecAnnotation = (seekbarProgress + 40).toString().padStart(3, '0') + "VL"

                if (arduino.AvRec.isOn) {
                    if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                        execute(volInRecAnnotation, listView)
                    }
                } else {
                    view.textView.text = resources.getString(R.string.textSeekbar)
                    view.croller.progress = 0
                    view.croller.indicatorColor = View.GONE
                    Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
                }
            }
        }
        croller.setOnCrollerChangeListener(obj)

        // respond to Lisas phone settings
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("app",
            Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isLargeAppearance", true)) {
            view.croller.layoutParams.height = 840
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        val connMgr = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
            val listView = requireView().media_list
            execute("getStatus", listView)
        } else {
            Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
        }
    }

    private fun execute(cmd: String, listView: ListView) {
        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(2, TimeUnit.SECONDS)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(arduinoBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO) {

            val response: Response<JsonObject>

            try {
                response = api.sendCommand(cmd).awaitResponse()

                if (response.isSuccessful) {
                    val data = response.body()
                    arduino.AvRec = Gson().fromJson(data, Arduino::class.java).AvRec
                    arduino.SkyRec = Gson().fromJson(data, Arduino::class.java).SkyRec
                }

                withContext(Dispatchers.Main) {
                    for (device in deviceList) {
                        (device as ListAction).isOn = false
                    }

                    if (arduino.AvRec.isOn) {
                        view!!.croller.indicatorColor =
                            resources.getColor(R.color.colorAccent, context?.theme)

                        when (arduino.AvRec.mode) {
                            // AvRec Modes: 1 = Sky, 2 = Chromecast, 4 = Bose
                            1 -> (deviceList[0] as ListAction).isOn = true
                            2 -> (deviceList[2] as ListAction).isOn = true
                            4 -> (deviceList[1] as ListAction).isOn = true
                        }

                        view!!.textView.text = arduino.AvRec.vol.trimStart('0')
                        view!!.croller.progress = arduino.AvRec.vol.toInt() - 40
                    } else {
                        view!!.textView.text = resources.getString(R.string.textSeekbar)
                        view!!.croller.progress = 0
                        view!!.croller.indicatorColor = View.GONE
                    }

                    (listView.adapter as MyListAdapter).notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    view!!.textView.text = resources.getString(R.string.textSeekbar)
                    view!!.croller.progress = 0
                    view!!.croller.indicatorColor = View.GONE
                }
            }
        }
    }
}