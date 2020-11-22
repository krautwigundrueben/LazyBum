package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.commandComponents.MyStromDimmerCommand
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.myStromDimmerDataClasses.Bulb
import com.maximo.lazybum.deviceComponents.statusClasses.DimmerStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class MyStromBulb(override val dUrl: String, override val dName: String): Device {

    lateinit var responseObj: Bulb

    private val initColor = 0xff0000ff.toString()
    private val toggleCommand = MyStromDimmerCommand("33000000", "rgb", "toggle", 2000)

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun status(pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, MyStromBulbApi::class.java)

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
            val request = RequestBuilder.buildRequest(dUrl, MyStromBulbApi::class.java)

            request.action(toggleCommand.action).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    suspend fun default(sCmd: String): Status {
        try {
            val jCmd = Gson().fromJson(sCmd, MyStromDimmerCommand::class.java)

            return suspendCoroutine { continuation ->
                val request = RequestBuilder.buildRequest(dUrl, MyStromBulbApi::class.java)

                request.set(jCmd.color, jCmd.mode, jCmd.action, jCmd.ramp).enqueue(object : Callback<JsonObject> {
                    override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        continuation.resume(processResponse(response))
                    }
                })
            }
        }
        catch (exception: Exception) {
            return DimmerStatus(false, initColor)
        }
    }

    private fun processResponse(response: Response<JsonObject>): Status {
        return try {
            val dataJson = response.body()?.getAsJsonObject("68C63AD026E9")
            responseObj = Gson().fromJson(dataJson, Bulb::class.java)
            DimmerStatus(responseObj.on, responseObj.color)
        } catch (exception: Exception) {
            DimmerStatus(false, initColor)
        }
    }

    override fun getType(): DeviceManager.DeviceType {
        return DeviceManager.DeviceType.DIMMER
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("default", ::default),
            Command("status", ::status),
            Command("toggle", ::toggle)
        )
    }
}

interface MyStromBulbApi {
    @GET("/api/v1/device")
    fun getStatus(): Call<JsonObject>

    @FormUrlEncoded
    @POST("/api/v1/device/68C63AD026E9")
    fun set(
        @Field("color") color: String,
        @Field("mode") mode: String,
        @Field("action") action: String,
        @Field("ramp") ramp: Int
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("/api/v1/device/68C63AD026E9")
    fun action(
        @Field("action") action: String
    ): Call<JsonObject>
}