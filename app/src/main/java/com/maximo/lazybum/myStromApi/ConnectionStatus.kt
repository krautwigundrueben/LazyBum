package com.maximo.lazybum.myStromApi

data class ConnectionStatus(
    val connection: Boolean,
    val dns: Boolean,
    val handshake: Boolean,
    val login: Boolean,
    val ntp: Boolean
)