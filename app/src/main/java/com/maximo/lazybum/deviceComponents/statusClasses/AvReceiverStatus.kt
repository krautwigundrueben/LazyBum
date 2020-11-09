package com.maximo.lazybum.deviceComponents.statusClasses

class AvReceiverStatus(isOn: Boolean, _mode: Int, volString: String): Status {

    private val modeMap: HashMap<Int, String> = hashMapOf(1 to "TV", 2 to "CCaudio", 4 to "Bose")

    override var isActive = isOn
    var mode: String = modeMap.get(_mode).toString()
    var vol: Int = volString.toInt()
}