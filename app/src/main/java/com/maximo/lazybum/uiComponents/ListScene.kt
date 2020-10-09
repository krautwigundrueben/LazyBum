package com.maximo.lazybum.uiComponents

data class ListScene(
    override val isHeader: Boolean = false,
    override val id: Long,
    override val text: String,
    override val img: Int,
    override val description: String,
    override var isOn: Boolean = false,
    val actionList: MutableList<ListAction>
): ListItem