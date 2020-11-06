package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.Relay
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.ShellyRelayStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ShellyRelay(override val dUrl: String, override val dName: String): Device {

    private val TAG = this.javaClass.toString()
    var deviceStatus: Relay? = null

    suspend fun status(pseudoParam: String): String {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyRelayApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    val data = response.body()
                    val status = Gson().fromJson(data, ShellyRelayStatus::class.java)

                    deviceStatus = status.relays[0]

                    val newStatus: String
                    when (status.relays[0].ison) {
                        true -> newStatus = "on"
                        else -> newStatus = "off"
                    }
                    continuation.resume(newStatus)
                }
            })
        }
    }

    suspend fun toggle(pseudoParam: String): String {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyRelayApi::class.java)

            request.set("toggle").enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    private fun processResponse(response: Response<JsonObject>): String {
        val data = response.body()
        deviceStatus = Gson().fromJson(data, Relay::class.java)

        if (!deviceStatus?.ison!!) return "off"
        else return "on"
    }

    override fun getType(): Int {
        return DeviceManager.DeviceType.shellyRelay.ordinal
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("status", ::status),
            Command("toggle", ::toggle)
        )
    }
}

interface ShellyRelayApi {
    @GET("/status")
    fun getStatus(): Call<JsonObject>

    @POST("/relay/0")
    fun set(
        @Query("turn") turn: String
    ): Call<JsonObject>
}