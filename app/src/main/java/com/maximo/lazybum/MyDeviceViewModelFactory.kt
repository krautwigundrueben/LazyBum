package com.maximo.lazybum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maximo.lazybum.deviceComponents.Command
import com.maximo.lazybum.deviceComponents.DeviceManager

@Suppress("UNCHECKED_CAST")
class MyDeviceViewModelFactory(
    val dName: String,
    val dInstance: Any,
    val dCommands: Array<Command>
): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceManager.DeviceViewModel::class.java)) {
            return DeviceManager.DeviceViewModel(dName, dInstance, dCommands) as T
        }
        throw IllegalArgumentException("ViewModel class not found.")
    }
}