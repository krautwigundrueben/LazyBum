package com.maximo.lazybum

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RequestBuilder {

    private val client = OkHttpClient()
        .newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addNetworkInterceptor(Interceptor {
            it.proceed(it.request()
                .newBuilder()
                .removeHeader("Accept-Encoding")
                .build()
            )
        })
        .connectTimeout(3, TimeUnit.SECONDS)
        .build()

    fun<T> buildRequest(url: String, request: Class<T>): T{
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(url)
            .client(client)
            .build()
            .create(request)
    }
}