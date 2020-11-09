package com.maximo.lazybum.deviceComponents.statusClasses

class SwitchStatus(isOn: Boolean): Status {

    override var isActive = isOn
}