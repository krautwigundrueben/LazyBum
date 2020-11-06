package com.maximo.lazybum.commandComponents

data class MyStromDimmerCommand(
    val color: String,
    val mode: String,
    val action: String,
    val ramp: Int
)