package com.maximo.lazybum.deviceComponents.statusClasses

import com.maximo.lazybum.deviceComponents.dataClasses.vacuumClasses.Zones

class VacuumStatus(state: Boolean, val zones: Zones): Status {

    override var isActive = state
}