package com.maximo.lazybum

data class LedGridCmd(
    override val action: String,
    val color: String,
    val mode: String,
    val ramp: Int
): CmdInterface