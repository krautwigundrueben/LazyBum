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
    val stop_reason: String,
    val source: String,
    val id: Int,
    val apower: Double,
    val voltage: Double,
    val current: Double,
    val pf: Double,
    val freq: Double,
    val aenergy: Aenergy,
    val temperature: Temperature,
    val pos_control: Boolean,
    val move_timeout: Double,
    val move_started_at: Double
)

data class Aenergy(
    val total: Double,
    val by_minute: List<Double>,
    val minute_ts: Long
)

data class Temperature(
    val tC: Double,
    val tF: Double
)