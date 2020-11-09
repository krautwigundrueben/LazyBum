package com.maximo.lazybum.deviceComponents

import com.maximo.lazybum.deviceComponents.statusClasses.Status
import kotlin.reflect.KSuspendFunction1

interface Device {
    val dUrl: String
    val dName: String

    fun getType(): DeviceManager.DeviceType
    fun getCommands(): Array<Command>
}

class Command(
    val cName: String,
    val cFunction: KSuspendFunction1<String, Status>,
)