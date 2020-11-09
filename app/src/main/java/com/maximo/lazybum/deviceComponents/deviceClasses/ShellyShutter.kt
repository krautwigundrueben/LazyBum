package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.ShellyShutter
import com.maximo.lazybum.deviceComponents.statusClasses.ShutterStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ShellyShutter(override val dUrl: String, override val dName: String): Device {

    lateinit var responseObj: ShellyShutter
    private var nextGo: String = "close"

    suspend fun status(pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyShutterApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun next(pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyShutterApi::class.java)

            request.go(nextGo).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun default(sCmd: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyShutterApi::class.java)

            request.go(sCmd).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    private fun determineNextGo(state: String?, lastDirection: String?) {
        if (state == "stop") {
            if (lastDirection == "close") {
                nextGo = "open"
            } else {
                nextGo = "close"
            }
        } else {
            nextGo = "stop"
        }
    }

    private fun processResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), ShellyShutter::class.java)
        determineNextGo(responseObj.state, responseObj.last_direction)
        return ShutterStatus(false, responseObj.last_direction)
    }

    override fun getType(): DeviceManager.DeviceType {
        return DeviceManager.DeviceType.SHUTTER
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("status", ::status),
            Command("next", ::next),
            Command("default", ::default)
        )
    }
}

interface ShellyShutterApi {
    @POST("/roller/0")
    fun go(
        @Query("go") go: String
    ): Call<JsonObject>

    @GET("/roller/0")
    fun getStatus(): Call<JsonObject>
}