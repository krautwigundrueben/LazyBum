package com.maximo.lazybum.Devices.arduinoApi

import com.maximo.lazybum.SectionItem

data class Device (
    val id: Int,
    val title: String,
    val img: Int,
    val url: String,
    var isOn: Boolean,
    val command: Command
): SectionItem