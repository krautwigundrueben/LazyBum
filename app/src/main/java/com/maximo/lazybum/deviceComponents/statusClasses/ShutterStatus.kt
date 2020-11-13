package com.maximo.lazybum.deviceComponents.statusClasses

class ShutterStatus(state: Boolean, val nextGo: String): Status {

    override var isActive = state
}