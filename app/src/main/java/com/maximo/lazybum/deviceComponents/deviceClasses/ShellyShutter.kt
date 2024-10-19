package com.maximo.lazybum.deviceComponents.deviceClasses

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.commandComponents.ShellyShutterCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.RequestBuilder
import com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses.Shutter
import com.maximo.lazybum.deviceComponents.statusClasses.ShutterStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ShellyShutter(override val dUrl: String, override val dName: String): Device {

    private lateinit var responseObj: Shutter
    private var nextGo: String = "close"

    private val nextGoMap: HashMap<String, String> = hashMapOf("stop" to "stop", "close" to "runter", "open" to "hoch")

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun status(deviceName: String, pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyShutterApi::class.java);
            var statusInterface = request.getStatus();
            if (dUrl == "http://192.168.178.59") statusInterface = request.getStatusV2();
            statusInterface.enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun next(deviceName: String, pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyShutterApi::class.java)

            request.go(nextGo, null).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponseModified(response))
                }
            })
        }
    }

    suspend fun default(deviceName: String, sCmd: String): Status {
        val jCmd = Gson().fromJson(sCmd, ShellyShutterCommand::class.java)

        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, ShellyShutterApi::class.java)

            request.go(jCmd.go, jCmd.roller_pos).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    private fun determineNextGo(state: String?, lastDirection: String?, moveStartedAt: Double): String {
        if (dUrl == "http://192.168.178.59") Log.e("state", "$state, $lastDirection, $moveStartedAt")
        return if (state == "stop" || state == "stopped") {
            if (lastDirection == "close") "open" else "close"
        } else if (nextGo != "stop" && moveStartedAt == 0.0 && dUrl == "http://192.168.178.59") {
            if (lastDirection == "close") "open" else "close"
        }
        else "stop"
    }

    private fun processResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), Shutter::class.java)
//        if(dUrl=="http://192.168.178.59") Log.e("next go", responseObj.toString())
        nextGo = determineNextGo(responseObj.state, responseObj.last_direction, responseObj.move_started_at)
        return if (!nextGoMap.get(nextGo).isNullOrBlank()) nextGoMap[nextGo]?.let {
            ShutterStatus(false, it)
        }!!
        else return ShutterStatus(false, "stop")
    }

    private fun processResponseModified(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), Shutter::class.java)
//        if(dUrl=="http://192.168.178.59") Log.e("next go", responseObj.toString())
        if (dUrl == "http://192.168.178.59") {
            nextGo = determineNextGo(nextGo, responseObj.last_direction, -1.0)
        } else nextGo = determineNextGo(responseObj.state, responseObj.last_direction, -1.0)
        return if (!nextGoMap.get(nextGo).isNullOrBlank()) nextGoMap[nextGo]?.let {
            ShutterStatus(false, it)
        }!!
        else return ShutterStatus(false, "stop")
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
    @GET("/roller/0")
    fun go(
        @Query("go") go: String,
        @Query("roller_pos") pos: String?
    ): Call<JsonObject>

    @GET("/roller/0")
    fun getStatus(): Call<JsonObject>

    @GET("/rpc/Cover.GetStatus?id=0")
    fun getStatusV2(): Call<JsonObject>
}