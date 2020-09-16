package com.maximo.lazybum.Devices.arduinoApi

data class Command(
    val action: String,
    val value: String,
    val color: String,
    val mode: String,
    val ramp: Int
)