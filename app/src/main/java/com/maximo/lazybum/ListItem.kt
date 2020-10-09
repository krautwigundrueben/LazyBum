package com.maximo.lazybum

interface ListItem: ListRow {
    val img: Int
    val description: String
    var isOn: Boolean
}