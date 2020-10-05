package com.maximo.lazybum

interface SectionItem: ListItem {
    override val isSectionHeader: Boolean get() = false
}