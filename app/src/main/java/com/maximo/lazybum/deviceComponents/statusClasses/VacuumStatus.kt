package com.maximo.lazybum.deviceComponents.statusClasses

class VacuumStatus(state: Boolean): Status {

    override var isActive = state
}