package com.maximo.lazybum

data class SectionHeader(
    override val isSectionHeader: Boolean = true,
    var title: String
): ListItem