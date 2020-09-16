package com.maximo.lazybum.shellyApi

data class WifiSta(
    val connected: Boolean,
    val ip: String,
    val rssi: Int,
    val ssid: String
)