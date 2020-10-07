package com.maximo.lazybum

data class SectionHeader(
    override val isSectionHeader: Boolean = true,
    override val id: Long,
    override var title: String
): ListItem