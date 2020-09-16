package com.maximo.lazybum.shellyApi

data class Relay(
    val has_timer: Boolean,
    val ison: Boolean,
    val source: String,
    val timer_duration: Int,
    val timer_remaining: Int,
    val timer_started: Int
)