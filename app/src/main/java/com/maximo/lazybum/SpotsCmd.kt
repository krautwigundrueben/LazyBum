package com.maximo.lazybum

data class SpotsCmd(
    override val action: String,
    val value: String
): CmdInterface