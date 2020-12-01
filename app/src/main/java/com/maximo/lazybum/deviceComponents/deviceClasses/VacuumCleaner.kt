package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.RequestBuilder
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.DeviceManager.DeviceType.VACUUM
import com.maximo.lazybum.deviceComponents.dataClasses.vacuumClasses.Vacuum
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.deviceComponents.statusClasses.VacuumStatus
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


data class VacuumCleaner(override val dUrl: String, override val dName: String): Device {

    private lateinit var responseObj: Vacuum
    private val spot = "{\"x\":18142,\"y\":29634}"

    fun isResponseInitialized(): Boolean {
        return this::responseObj.isInitialized
    }

    suspend fun default(deviceName: String, zone: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, VacuumApi::class.java)

            val mediaType: MediaType? = "application/json".toMediaTypeOrNull()
            val body: RequestBody = zone.toRequestBody(mediaType)

            request.zonedCleanup(body).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(VacuumStatus(false))
                }
            })
        }
    }

    suspend fun empty(deviceName: String, commandName: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, VacuumApi::class.java)

            val mediaType: MediaType? = "application/json".toMediaTypeOrNull()
            val body: RequestBody = spot.toRequestBody(mediaType)

            request.empty(body).enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(VacuumStatus(false))
                }
            })
        }
    }

    suspend fun home(deviceName: String, zone: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, VacuumApi::class.java)

            request.home().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(VacuumStatus(false))
                }
            })
        }
    }

    suspend fun stop(deviceName: String, zone: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, VacuumApi::class.java)

            request.stop().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(VacuumStatus(false))
                }
            })
        }
    }

    suspend fun status(deviceName: String, zone: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, VacuumApi::class.java)

            request.status().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processResponse(response))
                }
            })
        }
    }

    fun processResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), Vacuum::class.java)
        return VacuumStatus(false)
    }

    override fun getType(): DeviceManager.DeviceType {
        return VACUUM
    }

    override fun getCommands(): Array<Command> {
        return arrayOf(
            Command("default", ::default),
            Command("empty", ::empty),
            Command("home", ::home),
            Command("stop", ::stop),
            Command("status", ::status)
        )
    }
}

interface VacuumApi {
    @GET("/api/current_status")
    fun status(): Call<JsonObject>

    @PUT("/api/start_cleaning_zone")
    fun zonedCleanup(
        @Body body: RequestBody
    ): Call<JsonObject>

    @PUT("/api/stop_cleaning")
    fun stop(): Call<JsonObject>

    @PUT("/api/drive_home")
    fun home(): Call<JsonObject>

    @PUT("/api/go_to")
    fun empty(
        @Body body: RequestBody
    ): Call<JsonObject>
}