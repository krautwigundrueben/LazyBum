package com.maximo.lazybum.uiComponents

import com.maximo.lazybum.commands.CmdInterface

data class Action (
    val deviceId: Long,
    val url: String,
    val cmd: CmdInterface
)