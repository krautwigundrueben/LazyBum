package com.maximo.lazybum.deviceComponents.deviceClasses

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.ArduinoCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.arduinoDataClasses.AvRec
import com.maximo.lazybum.deviceComponents.dataClasses.arduinoDataClasses.MyArduino
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ArduinoAvReceiver(override val dUrl: String, override val dName: String): Device, ViewModel() {

    private val TAG = this.javaClass.toString()
    var deviceStatus: AvRec? = null
    private val modeMap: HashMap<Int, String> = hashMapOf(1 to "TV", 2 to "CCaudio", 4 to "Bose")

    val mode: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    // TODO: abhängig von Response Datengrundlage für Adapter anpassen: Croller

    suspend fun status(pseudoParam: String): String {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ArduinoAvReceiverApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun default(sCmd: String): String {
        try {
            val jCmd = Gson().fromJson(sCmd, ArduinoCommand::class.java)

            return suspendCoroutine { continuation ->
                val request = RequestBuilder.buildRequest(dUrl, ArduinoAvReceiverApi::class.java)

                request.set(jCmd.turn).enqueue(object : Callback<JsonObject> {
                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                    override fun onResponse(
                        call: Call<JsonObject>,
                        response: Response<JsonObject>
                    ) {
                        continuation.resume(processResponse(response))
                    }
                })
            }
        } catch (exception: Exception) {
            Log.e(TAG, exception.toString())
            return "error"
        }
    }

    private fun processResponse(response: Response<JsonObject>): String {
        val data = response.body()
        deviceStatus = Gson().fromJson(data, MyArduino::class.java).AvRec

        if (!deviceStatus?.isOn!!) return "off"
        else return modeMap.get(deviceStatus?.mode)!!
    }

    override fun getType(): Int {
        return DeviceManager.DeviceType.arduino.ordinal
    }

    override fun getCommands(): Array<Command> {

        return arrayOf(
            Command("status", ::status),
            Command("default", ::default)
        )
    }
}


interface ArduinoAvReceiverApi {
    @GET("/")
    fun getStatus(): Call<JsonObject>

    @POST("/")
    fun set(
        @Query("cmd") command: String
    ): Call<JsonObject>
}