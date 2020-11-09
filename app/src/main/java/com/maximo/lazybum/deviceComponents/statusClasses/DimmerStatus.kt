package com.maximo.lazybum.deviceComponents.statusClasses

class DimmerStatus(isOn: Boolean, var value: String): Status {

    override var isActive = isOn
}