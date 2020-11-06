package com.maximo.lazybum.deviceComponents

import kotlin.reflect.KSuspendFunction1

// TODO: mit DeviceClass zusammenlegen

interface Device {
    val dUrl: String
    val dName: String

    fun getType(): Int
    fun getCommands(): Array<Command>
}

class Command(
    val cName: String,
    val cFunction: KSuspendFunction1<String, String>,
)