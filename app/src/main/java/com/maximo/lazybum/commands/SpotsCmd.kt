package com.maximo.lazybum.commands

data class SpotsCmd(
    override val action: String,
    val value: String
): CmdInterface