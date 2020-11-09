package com.maximo.lazybum.deviceComponents.statusClasses

class ShutterStatus(state: Boolean, lastDirection: String): Status {

    override var isActive = state
}