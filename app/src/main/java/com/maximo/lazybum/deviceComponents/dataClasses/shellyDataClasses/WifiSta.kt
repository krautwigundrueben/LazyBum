package com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses

data class WifiSta(
    val connected: Boolean,
    val ip: String,
    val rssi: Int,
    val ssid: String
)