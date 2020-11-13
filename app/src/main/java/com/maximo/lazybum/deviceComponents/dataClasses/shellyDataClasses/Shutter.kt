package com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses

data class Shutter(
    val calibrating: Boolean,
    val current_pos: Int,
    val is_valid: Boolean,
    val last_direction: String,
    val overtemperature: Boolean,
    val positioning: Boolean,
    val power: Double,
    val safety_switch: Boolean,
    val state: String,
    val stop_reason: String
)