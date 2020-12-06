package com.maximo.lazybum.deviceComponents.statusClasses

class AvReceiverStatus(isOn: Boolean, _mode: Int, volString: String): Status {

    private val modeMap: HashMap<Int, String> = hashMapOf(1 to "06FN", 2 to "04FN", 4 to "01FN")

    override var isActive = false
    var mode: String = modeMap[1].toString()
    var vol: Int = 0

    init {
        try {
            isActive = isOn
            mode = modeMap[_mode].toString()
            vol = volString.toInt()
        } catch (exception: Exception) {
            vol = -1
        }
    }
}
