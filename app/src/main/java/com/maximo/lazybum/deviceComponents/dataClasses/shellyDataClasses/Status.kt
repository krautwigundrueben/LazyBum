package com.maximo.lazybum.deviceComponents.dataClasses.shellyDataClasses

data class Status(
    val actions_stats: ActionsStats,
    val calib_progress: Int,
    val cfg_changed_cnt: Int,
    val cloud: Cloud,
    val fs_free: Int,
    val fs_size: Int,
    val has_update: Boolean,
    val inputs: List<Input>,
    val lights: List<Light>,
    val loaderror: Boolean,
    val mac: String,
    val meters: List<Meter>,
    val mqtt: Mqtt,
    val overload: Boolean,
    val overtemperature: Boolean,
    val ram_free: Int,
    val ram_total: Int,
    val serial: Int,
    val time: String,
    val tmp: Tmp,
    val unixtime: Int,
    val update: Update,
    val uptime: Int,
    val wifi_sta: WifiSta
)