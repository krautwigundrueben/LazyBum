package com.maximo.lazybum.deviceComponents.dataClasses.myStromSwitchDataClasses

data class Relay(
    val Ws: Double = 0.0,
    val power: Int = 0,
    val relay: Boolean = false,
    val temperature: Double = 0.0
)