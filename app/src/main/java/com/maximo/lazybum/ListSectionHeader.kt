package com.maximo.lazybum

data class ListSectionHeader(
    override val isHeader: Boolean = true,
    override val id: Long,
    override val text: String
): ListRow