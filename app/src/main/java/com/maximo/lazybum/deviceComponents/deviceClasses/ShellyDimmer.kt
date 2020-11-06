package com.maximo.lazybum.deviceComponents.deviceClasses

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.ShellyDimmerCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.Light
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.ShellyLightsStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ShellyDimmer(override val dUrl: String, override val dName: String): Device {

    private val TAG = this.javaClass.toString()
    var deviceStatus: Light? = null

    suspend fun status(pseudoParam: String): String {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyDimmerApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    val data = response.body()
                    val status = Gson().fromJson(data, ShellyLightsStatus::class.java)

                    deviceStatus = status.lights[0]

                    val newStatus: String
                    if (!deviceStatus?.ison!!) newStatus = "off"
                    else newStatus = deviceStatus?.brightness.toString()

                    continuation.resume(newStatus)
                }
            })
        }
    }

    suspend fun default(sCmd: String): String {
        try {
            val jCmd = Gson().fromJson(sCmd, ShellyDimmerCommand::class.java)

            return suspendCoroutine { continuation ->
                val request = RequestBuilder.buildRequest(dUrl, ShellyDimmerApi::class.java)

                request.set(jCmd.turn, jCmd.brightness).enqueue(object : Callback<JsonObject> {
                    override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
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
        deviceStatus = Gson().fromJson(data, Light::class.java)

        if (!deviceStatus?.ison!!) return "off"
        else return deviceStatus?.brightness.toString()
    }

    override fun getType(): Int {
        return DeviceManager.DeviceType.shellyDimmer.ordinal
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("default", ::default),
            Command("status", ::status),
        )
    }
}

interface ShellyDimmerApi {
    @GET("/status")
    fun getStatus(): Call<JsonObject>

    @POST("/light/0")
    fun set(
        @Query("turn") turn: String,
        @Query("brightness") brightness: String
    ): Call<JsonObject>
}