package com.maximo.lazybum

import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import com.maximo.lazybum.layoutComponents.Group

object Globals {
    val supportedWifiSsids: List<String> = listOf("AndroidWifi", "DasWeltweiteInternetz", "DasWeltweiteInternetz5GHz")

    lateinit var globalDeviceManager: DeviceManager

    lateinit var devicesFragmentGroups: List<Group>
    lateinit var scenesFragmentGroups: List<Group>
    lateinit var shutterFragmentGroups: List<Group>
    lateinit var avReceiverFragmentGroups: List<Group>

    var myListAdapters: HashMap<Int, MyListAdapter> = hashMapOf()

    val DEVICES_TAB_POS = 0
    val SCENES_TAB_POS = 1
    val AVREC_TAB_POS = 2
    val SHUTTER_TAB_POS = 3
}