package com.maximo.lazybum.api

data class WifiSta(
    val connected: Boolean,
    val ip: String,
    val rssi: Int,
    val ssid: String
)