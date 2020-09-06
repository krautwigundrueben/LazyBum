package com.maximo.lazybum

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface ApiRequest {

    // MyStrom Devices; device id(s): 0, 1
    @GET("/report")
    fun getReport(): Call<JsonObject>

    @GET("/toggle")
    fun toggle(): Call<JsonObject>

    @GET("/api/v1/device")
    fun getInfo(): Call<JsonObject>

    @FormUrlEncoded
    @POST("/api/v1/device/840D8E3D9494")
    fun set(
        @Field("color") color: String,
        @Field("mode") mode: String,
        @Field("action") action: String,
        @Field("ramp") ramp: Int
    ): Call<JsonObject>

    // Shelly Devices; device id(s): 2, 3
    @GET("/status")
    fun getStatus(): Call<JsonObject>

    @POST("/light/0")
    fun set(
        @Query("turn") turn: String,
        @Query("brightness") brightness: String
    ): Call<JsonObject>

    @POST("/relay/0")
    fun set(
        @Query("turn") turn: String
    ): Call<JsonObject>
}