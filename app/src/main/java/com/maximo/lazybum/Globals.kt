package com.maximo.lazybum

import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.layoutComponents.Group

object Globals {
    const val DEVICES_TAB_POS = 0
    const val SCENES_TAB_POS = 1
    const val AVREC_TAB_POS = 2
    const val SHUTTER_TAB_POS = 3
    const val VACUUM_TAB_POS = 4

    val supportedWifiSSIDs: List<String> = listOf("AndroidWifi", "DasWeltweiteInternetz", "DasWeltweiteInternetz5GHz")

    lateinit var deviceManager: DeviceManager

    lateinit var devicesFragmentGroups: List<Group>
    lateinit var scenesFragmentGroups: List<Group>
    lateinit var avReceiverFragmentGroups: List<Group>
    lateinit var shutterFragmentGroups: List<Group>
    lateinit var vacuumFragmentGroups: List<Group>
}