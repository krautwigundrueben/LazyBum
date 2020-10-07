package com.maximo.lazybum.Devices.arduinoApi
import com.maximo.lazybum.ListItem

data class Device (
    override val isSectionHeader: Boolean = false,
    override val id: Long,
    override val title: String,
    val img: Int,
    val url: String,
    var isOn: Boolean = false,
    val command: Command
): ListItem