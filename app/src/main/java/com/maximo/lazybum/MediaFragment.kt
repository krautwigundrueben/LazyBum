package com.maximo.lazybum

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.Devices.arduinoApi.*
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener
import kotlinx.android.synthetic.main.fragment_media.view.*
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

const val arduinoBaseUrl = "http://192.168.178.108"

class MediaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val arduino = Arduino(AvRec(false, 1, "aus"), SkyRec(false))

        val deviceList = mutableListOf<Device>()
        deviceList.addAll(listOf(skyReceiver, boseSoundtouch, chromecast, allOff))

        val view = inflater.inflate(R.layout.fragment_media, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(), R.layout.row, deviceList)

        listView.setOnItemClickListener { parent, v, position, id ->
            execute(arduino, deviceList[position].command.action, listView)
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

                execute(arduino, volInRecAnnotation, listView)
            }
        }
        croller.setOnCrollerChangeListener(obj)

        // get status and set status colors anew
        execute(arduino, "getStatus", listView)

        // respond to Lisas phone settings
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("app",
            Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isLargeAppearance", true)) {
            view.croller.layoutParams.height = 840
        }

        return view
    }

    private fun execute(arduino: Arduino, cmd: String, listView: ListView) {

        // reset colors to off
        for (itemView in listView) itemView.imageView.setColorFilter(ContextCompat.getColor(
            requireContext(), R.color.colorOff), PorterDuff.Mode.SRC_IN)

        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
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
                    if (arduino.AvRec.isOn) {
                        view!!.croller.indicatorColor = resources.getColor(R.color.colorAccent, context?.theme)

                        when (arduino.AvRec.mode) {
                            // AvRec Modes: 1 = Sky, 3 = Chromecast, 4 = Bose
                            1 -> listView.getChildAt(0).imageView.setColorFilter(ContextCompat.getColor(
                                requireContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                            2 -> listView.getChildAt(2).imageView.setColorFilter(ContextCompat.getColor(
                                requireContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                            4 -> listView.getChildAt(1).imageView.setColorFilter(ContextCompat.getColor(
                                requireContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                        }
                        view!!.textView.text = arduino.AvRec.vol.trimStart('0')
                        view!!.croller.progress = arduino.AvRec.vol.toInt() - 40
                    }
                    else {
                        view!!.textView.text = resources.getString(R.string.textSeekbar)
                        view!!.croller.progress = 0
                        view!!.croller.indicatorColor = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {

        private val skyReceiver = Device(0,"Sky Receiver","Wohnzimmer", R.drawable.ic_monitor,
            arduinoBaseUrl,false, Command("TV", "", "", "", 0))

        private val boseSoundtouch = Device(1,"Bose Soundtouch","Wohnzimmer", R.drawable.ic_music,
            arduinoBaseUrl,false, Command("Bose", "", "", "", 0))

        private val chromecast = Device(2,"Chromecast","Wohnzimmer", R.drawable.ic_film,
            arduinoBaseUrl,false, Command("CCaudio", "", "", "", 0))

        private val allOff = Device(3,"Alles ausschalten","Wohnzimmer", R.drawable.ic_power_off,
            arduinoBaseUrl,false, Command("DvcsOff", "", "", "", 0))
    }
}