package com.maximo.lazybum.layoutComponents

data class LayoutGroup(
    val header: String,
    val items: List<LayoutItem>
) {
    fun toGroup(): Group {
        val itemList: MutableList<Item> = mutableListOf()
        for (item in items) {
            itemList.add(item.toItem())
        }
        return Group(header, itemList)
    }
}