package com.maximo.lazybum

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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

class MediaFragment : Fragment() {

    val TAG = "MediaFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val arduino = Device(
            108,
            "Arduino",
            "Wohnzimmer",
            R.drawable.ic_monitor,
            "http://192.168.178.108",
            false,
            Command("", "", "", "", 0)
        )

        val deviceList = mutableListOf<Device>()

        deviceList.add(
            Device(
                5,
                "Sky fernsehen",
                "Wohnzimmer",
                R.drawable.ic_monitor,
                arduino.base_url,
                false,
                Command("TV", "", "", "", 0)
            )
        )
        deviceList.add(
            Device(
                6,
                "Bose Soundtouch",
                "Wohnzimmer",
                R.drawable.ic_monitor,
                arduino.base_url,
                false,
                Command("Bose", "", "", "", 0)
            )
        )
        deviceList.add(
            Device(
                99,
                "Alles ausschalten",
                "Wohnzimmer",
                R.drawable.ic_coffee,
                arduino.base_url,
                false,
                Command("DvcsOff", "", "", "", 0)
            )
        )

        val view = inflater.inflate(R.layout.fragment_media, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(), R.layout.row, deviceList)

        val croller: Croller = view.croller

        val obj = object : OnCrollerChangeListener {

            override fun onProgressChanged(croller: Croller?, progress: Int) { }
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

                execute(arduino, Command(volInRecAnnotation, "", "", "", 0))
            }
        }
        croller.setOnCrollerChangeListener(obj)

        return view
    }

    private fun execute(device: Device, command: Command) {

        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val api = Retrofit.Builder()
            .baseUrl(device.base_url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO) {

            val response: Response<StringBuffer>

            try {
                response = api.sendCommand(command.action).awaitResponse()

                if (response.isSuccessful) {
                    val data = response.body()
                    println(data)
                } else {
                    Log.d(TAG, response.body().toString())
                    Log.d(TAG, response.code().toString())
                }

                withContext(Dispatchers.Main) {

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //Toast.makeText(mCtx, "Something went wrong", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

/*
BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((state = bufferedReader.readLine()) != null && !state.isEmpty()) {
                // states werden ';'-getrennt, z. B. Beamer_On=true;AVRec_On=false;...
                decodedResponse = decodedResponse + state + ";";
            }
        } catch (Exception e) {
        }

        return decodedResponse;
    }

    protected void onPostExecute(String result) {

        // jede Response enthält aktuelle states (';'-getrennt), welche
        // als Boolean an App-Variablen übergeben werden
        String[] states = result.split(";");
        Boolean[] statesBool = new Boolean[states.length];

        for (int i = 0; i < states.length; i++) {
            if (states[i].contains("false")) statesBool[i] = false;
            else statesBool[i] = true;
        }

        MainActivity.AVRec_On = statesBool[0];
        MainActivity.SkyRec_On = statesBool[1];
        MainActivity.Beamer_On = statesBool[2];
        MainActivity.lDesk_On = statesBool[3];
        MainActivity.lFloor_On = statesBool[4];
 */