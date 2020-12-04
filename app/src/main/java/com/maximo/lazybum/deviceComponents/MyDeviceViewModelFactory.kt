package com.maximo.lazybum.deviceComponents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class MyDeviceViewModelFactory(
    val dName: String,
    private val dInstance: Any,
    private val dCommands: Array<Command>
): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceManager.DeviceViewModel::class.java)) {
            return DeviceManager.DeviceViewModel(dName, dInstance, dCommands) as T
        }
        throw IllegalArgumentException("ViewModel Klasse nicht gefunden.")
    }
}