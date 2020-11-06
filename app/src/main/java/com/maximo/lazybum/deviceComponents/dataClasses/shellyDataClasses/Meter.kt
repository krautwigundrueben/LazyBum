package com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses

data class Meter(
    val counters: List<Double>,
    val is_valid: Boolean,
    val power: Double,
    val timestamp: Int,
    val total: Int
)