package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.ArduinoCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.arduinoDataClasses.ArduinoResponseJson
import com.maximo.lazybum.deviceComponents.dataClasses.arduinoDataClasses.SkyReceiverJson
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.deviceComponents.statusClasses.SwitchStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ArduinoSkyReceiver(override val dUrl: String, override val dName: String): Device {

    private lateinit var responseObj: SkyReceiverJson

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun status(deviceName: String, pseudoParam: String): Status {
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

    suspend fun default(deviceName: String, sCmd: String): Status {
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
            return SwitchStatus(false)
        }
    }

    private fun processResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), ArduinoResponseJson::class.java).SkyRec
        return SwitchStatus(responseObj.isOn)
    }

    override fun getType(): DeviceManager.DeviceType {
        return DeviceManager.DeviceType.SWITCH
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