package com.maximo.lazybum

class Device (
    val id: Int,
    val title: String,
    val location: String,
    val img: Int,
    val base_url: String,
    var isOn: Boolean,
    val command: Command
)