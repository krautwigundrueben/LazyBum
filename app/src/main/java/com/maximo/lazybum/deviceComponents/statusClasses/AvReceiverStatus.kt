package com.maximo.lazybum.deviceComponents.statusClasses

class AvReceiverStatus(isOn: Boolean, _mode: Int, volString: String): Status {

    private val modeMap: HashMap<Int, String> = hashMapOf(1 to "TV", 2 to "CCaudio", 4 to "Bose")

    override var isActive = false
    var mode: String = modeMap[1].toString()
    var vol: Int = 0

    init {
        try {
            isActive = isOn
            mode = modeMap[_mode].toString()
            vol = volString.toInt()
        } catch (exception: Exception) {
        }
    }
}
