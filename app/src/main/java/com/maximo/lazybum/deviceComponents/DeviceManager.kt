package com.maximo.lazybum.deviceComponents

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maximo.lazybum.Globals
import com.maximo.lazybum.MainActivity
import com.maximo.lazybum.MyDeviceViewModelFactory
import com.maximo.lazybum.deviceComponents.dataClasses.DeviceClass
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.deviceComponents.statusClasses.SwitchStatus
import com.maximo.lazybum.layoutComponents.Action
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.reflect.full.callSuspend

class DeviceManager(mainActivity: MainActivity, devices: List<DeviceClass>) {

    class DeviceViewModel(val dName: String, val dInstance: Any, val dCommands: Array<Command>): ViewModel() {
        lateinit var deviceType: DeviceType
        private val _status: MutableLiveData<Status> by lazy { MutableLiveData<Status>() }
        fun getStatus(): LiveData<Status> { return _status }
        fun setStatus(newStatus: Status) { _status.postValue(newStatus) }
    }

    enum class DeviceType { AV_RECEIVER, DIMMER, SWITCH, SHUTTER }
    private val myDevices = mutableListOf<DeviceViewModel>()

    init {
        // loadDevices()

        for (device in devices) {
            registerDevice(mainActivity, device)

            GlobalScope.launch {
                getInitialStatus(mainActivity, Action("status", device.dName))
            }
        }
    }

    fun registerDevice(mainActivity: MainActivity, newDevice: DeviceClass) { //: MutableList<DeviceViewModel>
        /*
        val myDevices: MutableList<DeviceViewModel> = mutableListOf()

        for (newDevice in initialDeviceList) {

         */

        val kClass =
            Class.forName("com.maximo.lazybum.deviceComponents.deviceClasses." + newDevice.dType).kotlin
        val instance =
            kClass.constructors.first().call(newDevice.dUrl, newDevice.dName) as Device

        val viewModelFactory = MyDeviceViewModelFactory(instance.dName, instance, instance.getCommands())
        val viewModel = ViewModelProvider(mainActivity, viewModelFactory).get(instance.dName, DeviceViewModel::class.java)
        viewModel.deviceType = instance.getType()
        myDevices.add(viewModel)

        //return myDevices
    }

    fun getDevice(deviceName: String): DeviceViewModel? {
        return myDevices.find { it.dName == deviceName }
    }

    private suspend fun getInitialStatus(mainActivity: MainActivity, action: Action) {
        val connMgr = mainActivity.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
            try {
                executeCommand(action)
            } catch (e: Exception) {
                Log.e("DeviceManager", e.message.toString())
            }
        }
    }

    suspend fun executeCommand(action: Action) {
        val targetDevice = getDevice(action.deviceName)
        val targetFunction =
            targetDevice?.dCommands!!.find { it.cName.startsWith(action.commandName) }?.cFunction

        var response: Status = SwitchStatus(false)

        GlobalScope.async(IO) {
            if (targetFunction != null) {
                response = targetFunction.callSuspend(action.commandName) // targetDevice.dStatus =
            }
            else { // must be an appropriate Command Json then
                response = targetDevice.dCommands.find { it.cName == "default" }?.cFunction?.callSuspend(action.commandName)!! // targetDevice.dStatus =
            }
        }.await()

        targetDevice.setStatus(response)
    }

/*
    private fun loadDevices() {

        val deviceConfigFile = this::class.java.getResourceAsStream("/res/raw/devices_config.json")

        val listDeviceType = object : TypeToken<List<DeviceClass>>() {}.type
        val initialDeviceList: List<DeviceClass> =
            Gson().fromJson(InputStreamReader(deviceConfigFile), listDeviceType)
        devices.value = registerDevices(initialDeviceList)
        //Globals.globalDeviceManager = DeviceManager(initialDeviceList)

    }
*/
}