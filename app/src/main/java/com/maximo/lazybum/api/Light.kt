package com.maximo.lazybum.api

data class Light(
    val brightness: Int,
    val has_timer: Boolean,
    val ison: Boolean,
    val mode: String,
    val timer_duration: Int,
    val timer_remaining: Int,
    val timer_started: Int
)