package com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses

data class Update(
    val has_update: Boolean,
    val new_version: String,
    val old_version: String,
    val status: String
)