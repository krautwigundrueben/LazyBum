package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.ArduinoCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
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

    lateinit var responseObj: Relay

    private val switchMap: HashMap<String, Int> = hashMapOf("on" to 1, "off" to 0)

    suspend fun status(pseudoParam: String): Status {
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

    suspend fun toggle(pseudoParam: String): Status {
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

    suspend fun switch(sCmd: String): Status {
        val jCmd = Gson().fromJson(sCmd, ArduinoCommand::class.java)

        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, MyStromSwitchApi::class.java)

            request.switch(switchMap.get(jCmd.turn)!!).enqueue(object : Callback<Void> {
                override fun onFailure(call: Call<Void>, t: Throwable) { }

                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    continuation.resume(SwitchStatus(true))
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