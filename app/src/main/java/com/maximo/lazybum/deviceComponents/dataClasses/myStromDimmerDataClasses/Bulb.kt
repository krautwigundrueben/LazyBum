package com.maximo.lazybum.deviceComponents.dataClasses.myStromDimmerDataClasses

data class Bulb(
    val battery: Boolean,
    var color: String,
    val connectionStatus: ConnectionStatus,
    val fw_version: String,
    val meshroot: Boolean,
    val mode: String,
    var on: Boolean,
    val power: Int,
    val ramp: Int,
    val reachable: Boolean,
    val type: String
)