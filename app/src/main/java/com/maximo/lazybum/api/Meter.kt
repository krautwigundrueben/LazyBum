package com.maximo.lazybum.api

data class Meter(
    val counters: List<Double>,
    val is_valid: Boolean,
    val power: Double,
    val timestamp: Int,
    val total: Int
)