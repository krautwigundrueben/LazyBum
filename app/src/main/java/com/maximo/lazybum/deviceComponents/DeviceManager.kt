package com.maximo.lazybum.deviceComponents

import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.maximo.lazybum.Globals
import com.maximo.lazybum.MainActivity
import com.maximo.lazybum.MyDeviceViewModelFactory
import com.maximo.lazybum.R
import com.maximo.lazybum.deviceComponents.dataClasses.DeviceClass
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.deviceComponents.statusClasses.SwitchStatus
import com.maximo.lazybum.layoutComponents.Action
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.reflect.full.callSuspend

class DeviceManager(mainActivity: MainActivity) {

    class DeviceViewModel(val dName: String, val dInstance: Any, val dCommands: Array<Command>): ViewModel() {
        lateinit var deviceType: DeviceType
        private val _status: MutableLiveData<Status> by lazy { MutableLiveData<Status>() }
        fun getStatus(): LiveData<Status> { return _status }
        fun setStatus(newStatus: Status) { _status.postValue(newStatus) }
    }

    enum class DeviceType { AV_RECEIVER, DIMMER, SWITCH, SHUTTER, VACUUM }
    val myDevices = mutableListOf<DeviceViewModel>()

    init {
        val devices: List<DeviceClass> = readDeviceConfigFile(mainActivity)

        for (device in devices) {
            registerDevice(mainActivity, device)
        }
    }

    private fun registerDevice(mainActivity: MainActivity, newDevice: DeviceClass) {
        val kClass =
            Class.forName(mainActivity.getString(R.string.device_classes_location) + newDevice.dType).kotlin
        val instance =
            kClass.constructors.first().call(newDevice.dUrl, newDevice.dName) as Device

        val viewModelFactory = MyDeviceViewModelFactory(instance.dName, instance, instance.getCommands())
        val viewModel = ViewModelProvider(mainActivity, viewModelFactory).get(instance.dName, DeviceViewModel::class.java)
        viewModel.deviceType = instance.getType()
        myDevices.add(viewModel)
    }

    fun getDevice(deviceName: String): DeviceViewModel? {
        return myDevices.find { it.dName == deviceName }
    }

    fun launchAction(context: Context, action: Action) {
        val connMgr = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSSIDs.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
            try {
                GlobalScope.launch { executeCommand(action) }
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.not_at_home), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private suspend fun executeCommand(action: Action) {
        val targetDevice = getDevice(action.deviceName)
        val targetFunction =
            targetDevice?.dCommands!!.find { it.cName.startsWith(action.commandName) }?.cFunction

        var response: Status = SwitchStatus(false)

        withContext(IO) {
            response = targetFunction?.callSuspend(action.deviceName, action.commandName)
                ?: // must be an appropriate Command Json then
                        targetDevice.dCommands.find { it.cName == "default" }?.cFunction?.callSuspend(
                            action.deviceName, action.commandName)!!
        }

        targetDevice.setStatus(response)
    }


    private fun readDeviceConfigFile(mainActivity: MainActivity): List<DeviceClass> {
        val deviceConfigFile = this::class.java.getResourceAsStream(mainActivity.getString(R.string.devices_config_location))
        val listDeviceType = object : TypeToken<List<DeviceClass>>() {}.type
        return Gson().fromJson(InputStreamReader(deviceConfigFile), listDeviceType)
    }

}