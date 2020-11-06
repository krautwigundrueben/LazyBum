package com.maximo.lazybum.deviceComponents

import com.maximo.lazybum.deviceComponents.dataClasses.DeviceClass
import com.maximo.lazybum.layoutComponents.Action
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.reflect.full.callSuspend

class DeviceManager(devices: List<DeviceClass>) { // : ViewModel()

    val myDevices = mutableListOf<MyDevice>()
    private val initialStatus = "off"
    /*
    val devices: MutableLiveData<List<MyDevice>> by lazy {
        MutableLiveData<List<MyDevice>>()
    }

    val status: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    https://stackoverflow.com/questions/46283981/android-viewmodel-additional-arguments
    https://stackoverflow.com/questions/47941537/notify-observer-when-item-is-added-to-list-of-livedata
    https://developer.android.com/topic/libraries/architecture/viewmodel
    https://www.youtube.com/watch?v=N7J27pBTtuI
     */

    class MyDevice(
        val dName: String,
        val dInstance: Any,
        val dCommands: Array<Command>,
        var dStatus: String
    )

    init {
        // loadDevices()

        for (device in devices) {
            registerDevice(device)
        }
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

    fun getDevices(): LiveData<List<MyDevice>> {
        return devices
    }
*/

    enum class DeviceType {
        myStromSwitch, shellyRelay, myStromDimmer, shellyDimmer, arduino, shellyShutter
    }

    fun getDevice(deviceName: String): MyDevice? {
        //return devices.value?.find { it.dName == deviceName }
        return myDevices.find { it.dName == deviceName }
    }

    fun registerDevice(newDevice: DeviceClass) { //: MutableList<MyDevice>
        /*
        val myDevices: MutableList<MyDevice> = mutableListOf()

        for (newDevice in initialDeviceList) {

         */

            val kClass =
                Class.forName("com.maximo.lazybum.deviceComponents.deviceClasses." + newDevice.dType).kotlin
            val instance =
                kClass.constructors.first().call(newDevice.dUrl, newDevice.dName) as Device

            myDevices.add(MyDevice(instance.dName,
                instance,
                instance.getCommands(),
                initialStatus))

        //return myDevices
    }

    suspend fun executeCommand(action: Action) {
        //val targetDevice = devices.value?.find { it.dName == action.deviceName }
        val targetDevice = myDevices.find { it.dName == action.deviceName }
        val targetFunction =
            targetDevice?.dCommands!!.find { it.cName.startsWith(action.commandName) }?.cFunction

        GlobalScope.async(IO) {
            if (targetFunction != null) {
                targetDevice.dStatus = targetFunction.callSuspend(action.commandName)
            }
            else { // must be an appropriate Command Json then
                targetDevice.dStatus = targetDevice.dCommands.find { it.cName == "default" }?.cFunction?.callSuspend(action.commandName)!!
            }
        }.await()
    }
}