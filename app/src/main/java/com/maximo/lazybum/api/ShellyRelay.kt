package com.maximo.lazybum.api

data class ShellyRelay(
    val has_timer: Boolean,
    val ison: Boolean,
    val source: String,
    val timer_duration: Int,
    val timer_remaining: Int,
    val timer_started: Int
)