package com.maximo.lazybum.Devices.arduinoApi

data class Device (
    val id: Int,
    val title: String,
    val location: String,
    val img: Int,
    val url: String,
    var isOn: Boolean,
    val command: Command
)