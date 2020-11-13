package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.ShellyDimmerCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.Light
import com.maximo.lazybum.deviceComponents.statusClasses.DimmerStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ShellyDimmer(override val dUrl: String, override val dName: String): Device {

    lateinit var responseObj: Light

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun status(pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyDimmerApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun default(sCmd: String): Status {
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
            return DimmerStatus(false, "0")
        }
    }

    private fun processResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), Light::class.java)
        return DimmerStatus(responseObj.ison, responseObj.brightness.toString())
    }

    override fun getType(): DeviceManager.DeviceType {
        return DeviceManager.DeviceType.DIMMER
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("default", ::default),
            Command("status", ::status),
        )
    }
}

interface ShellyDimmerApi {
    @GET("/light/0")
    fun getStatus(): Call<JsonObject>

    @POST("/light/0")
    fun set(
        @Query("turn") turn: String,
        @Query("brightness") brightness: String
    ): Call<JsonObject>
}