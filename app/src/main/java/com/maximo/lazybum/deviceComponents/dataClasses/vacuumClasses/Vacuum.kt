package com.maximo.lazybum.deviceComponents.dataClasses.vacuumClasses

data class Vacuum(
    val battery: Int,
    val clean_area: Int,
    val clean_time: Int,
    val dnd_enabled: Int,
    val error_code: Int,
    val fan_power: Int,
    val human_error: String,
    val human_state: String,
    val in_cleaning: Int,
    val in_fresh_state: Int,
    val in_returning: Int,
    val lab_status: Int,
    val lock_status: Int,
    val map_present: Int,
    val map_status: Int,
    val model: String,
    val msg_ver: Int,
    val state: Int,
    val water_box_status: Int
)