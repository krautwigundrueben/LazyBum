package com.maximo.lazybum.layoutComponents

data class LayoutItem (
    val mainText: String,
    val subText: String,
    val icon: String,
    val actions: List<Action>
) {
    fun toItem(): Item {
        return Item(mainText, subText, icon, actions)
    }
}