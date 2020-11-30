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
import retrofit2.http.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class MyStromDimmer(override val dUrl: String, override val dName: String): Device {

    lateinit var responseObj: Bulb
    private val initColor = 0xff0000ff.toString()

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun status(deviceName: String, pseudoParam: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, MyStromDimmerApi::class.java)

            request.getStatus().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response, deviceName))
                }
            })
        }
    }

    suspend fun default(deviceName: String, sCmd: String): Status {
        try {
            val jCmd = Gson().fromJson(sCmd, MyStromDimmerCommand::class.java)

            return suspendCoroutine { continuation ->
                val request = RequestBuilder.buildRequest(dUrl, MyStromDimmerApi::class.java)

                request.set(deviceName, jCmd.color, jCmd.mode, jCmd.action, jCmd.ramp).enqueue(object : Callback<JsonObject> {
                    override fun onFailure(call: Call<JsonObject>, t: Throwable) { }

                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                        continuation.resume(processResponse(response, deviceName))
                    }
                })
            }
        }
        catch (exception: Exception) {
            return DimmerStatus(false, initColor)
        }
    }

    private fun processResponse(response: Response<JsonObject>, deviceName: String): Status {
        return try {
            val dataJson = response.body()?.getAsJsonObject(deviceName)
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
        )
    }
}

interface MyStromDimmerApi {
    @GET("/api/v1/device")
    fun getStatus(): Call<JsonObject>

    @FormUrlEncoded
    @POST("/api/v1/device/{deviceName}")
    fun set(
        @Path("deviceName") deviceName: String,
        @Field("color") color: String,
        @Field("mode") mode: String,
        @Field("action") action: String,
        @Field("ramp") ramp: Int
    ): Call<JsonObject>
}