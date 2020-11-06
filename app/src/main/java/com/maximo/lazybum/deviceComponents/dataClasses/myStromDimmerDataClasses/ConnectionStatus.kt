package com.maximo.lazybum.deviceComponents.dataClasses.myStromDimmerDataClasses

data class ConnectionStatus(
    val connection: Boolean,
    val dns: Boolean,
    val handshake: Boolean,
    val login: Boolean,
    val ntp: Boolean
)