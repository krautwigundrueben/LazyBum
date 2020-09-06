package com.maximo.lazybum.api

data class ShellyRelayStatus(
    val actions_stats: ActionsStats,
    val cfg_changed_cnt: Int,
    val cloud: Cloud,
    val ext_humidity: ExtHumidity,
    val ext_sensors: ExtSensors,
    val ext_temperature: ExtTemperature,
    val fs_free: Int,
    val fs_size: Int,
    val has_update: Boolean,
    val inputs: List<Input>,
    val mac: String,
    val meters: List<Meter>,
    val mqtt: Mqtt,
    val ram_free: Int,
    val ram_total: Int,
    val relays: List<Relay>,
    val serial: Int,
    val time: String,
    val unixtime: Int,
    val update: Update,
    val uptime: Int,
    val wifi_sta: WifiSta
)