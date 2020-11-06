package com.maximo.lazybum.deviceComponents.deviceClasses

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.ArduinoCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.arduinoDataClasses.MyArduino
import com.maximo.lazybum.deviceComponents.dataClasses.arduinoDataClasses.SkyRec
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ArduinoSkyReceiver(override val dUrl: String, override val dName: String): Device {

    private val TAG = this.javaClass.toString()
    var deviceStatus: SkyRec? = null

    suspend fun status(pseudoParam: String): String {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ArduinoSkyReceiverApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

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
                val request = RequestBuilder.buildRequest(dUrl, ArduinoSkyReceiverApi::class.java)

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
        }
        catch (exception: Exception) {
            Log.e(TAG, exception.toString())
            return "error"
        }
    }

    private fun processResponse(response: Response<JsonObject>): String {
        val data = response.body()
        deviceStatus = Gson().fromJson(data, MyArduino::class.java).SkyRec

        if (!deviceStatus?.isOn!!) return "off"
        else return "on"
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

interface ArduinoSkyReceiverApi {
    @GET("/")
    fun getStatus(): Call<JsonObject>

    @POST("/")
    fun set(
        @Query("cmd") command: String
    ): Call<JsonObject>
}