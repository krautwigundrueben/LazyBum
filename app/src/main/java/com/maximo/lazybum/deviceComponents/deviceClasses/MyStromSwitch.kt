package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.commandComponents.ArduinoCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.RequestBuilder
import com.maximo.lazybum.deviceComponents.dataClasses.myStromSwitchDataClasses.Relay
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.deviceComponents.statusClasses.SwitchStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class MyStromSwitch(override val dUrl: String, override val dName: String): Device {

    private lateinit var responseObj: Relay

    private val switchMap: HashMap<String, Int> = hashMapOf("on" to 1, "off" to 0)

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun status(deviceName: String, pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, MyStromSwitchApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun toggle(deviceName: String, pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, MyStromSwitchApi::class.java)

            request.toggle().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun switch(deviceName: String, sCmd: String): Status {
        val jCmd = Gson().fromJson(sCmd, ArduinoCommand::class.java)

        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, MyStromSwitchApi::class.java)

            request.switch(switchMap[jCmd.turn]!!).enqueue(object : Callback<Void> {
                override fun onFailure(call: Call<Void>, t: Throwable) { }

                override fun onResponse(call: Call<Void>, response: Response<Void>) {

                    val newStatus: Status = if (jCmd.turn == "on") {
                        SwitchStatus(true)
                    } else {
                        SwitchStatus(false)
                    }
                    continuation.resume(newStatus)
                }
            })
        }
    }

    private fun processResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), Relay::class.java)
        return SwitchStatus(responseObj.relay)
    }

    override fun getType(): DeviceManager.DeviceType {
        return DeviceManager.DeviceType.SWITCH
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("status", ::status),
            Command("toggle", ::toggle),
            Command("default", ::switch)
        )
    }
}

interface MyStromSwitchApi {
    @GET("/report")
    fun getStatus(): Call<JsonObject>

    @GET("/toggle")
    fun toggle(): Call<JsonObject>

    @GET("/relay")
    fun switch(
        // state = 1 = on
        @Query("state") state: Int
    ): Call<Void>
}