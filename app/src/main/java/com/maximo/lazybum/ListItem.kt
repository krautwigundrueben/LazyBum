package com.maximo.lazybum

interface ListItem {
    val isSectionHeader: Boolean
    val id: Long
    val title: String
}