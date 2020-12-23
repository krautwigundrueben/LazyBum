package com.maximo.lazybum.deviceComponents.deviceClasses

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.Device
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.DeviceManager.DeviceType.VACUUM
import com.maximo.lazybum.deviceComponents.RequestBuilder
import com.maximo.lazybum.deviceComponents.dataClasses.vacuumClasses.Vacuum
import com.maximo.lazybum.deviceComponents.dataClasses.vacuumClasses.Zones
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
    private val spot = "{\"x\":29927,\"y\":24410}"

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
                    continuation.resume(processStatusResponse(response))
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
                    continuation.resume(processStatusResponse(response))
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
                    continuation.resume(processStatusResponse(response))
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
                    continuation.resume(processStatusResponse(response))
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
                    continuation.resume(processStatusResponse(response))
                }
            })
        }
    }

    fun processStatusResponse(response: Response<JsonObject>): Status {
        responseObj = Gson().fromJson(response.body(), Vacuum::class.java)
        return VacuumStatus(false, Zones())
    }

    suspend fun zones(deviceName: String, zone: String): Status {
        return suspendCoroutine { continuation ->
            val request = RequestBuilder.buildRequest(dUrl, VacuumApi::class.java)

            request.zones().enqueue(object : Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}

                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    continuation.resume(processZonesResponse(response))
                }
            })
        }
    }

    fun processZonesResponse(response: Response<JsonObject>): Status {
        val zonesResponseObj: Zones = Gson().fromJson(response.body(), Zones::class.java)
        return VacuumStatus(false, zonesResponseObj)
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
            Command("status", ::status),
            Command("zones", ::zones)
        )
    }
}

interface VacuumApi {
    @GET("/api/current_status")
    fun status(): Call<JsonObject>

    @GET("/api/zones")
    fun zones(): Call<JsonObject>

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