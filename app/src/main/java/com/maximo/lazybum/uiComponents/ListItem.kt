package com.maximo.lazybum.uiComponents

interface ListItem: ListRow {
    val img: Int
    val description: String
    var isOn: Boolean
}