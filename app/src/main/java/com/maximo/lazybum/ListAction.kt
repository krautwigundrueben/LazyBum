package com.maximo.lazybum

data class ListAction (
    override val isHeader: Boolean = false,
    override val id: Long,
    override val text: String,
    override val img: Int,
    override val description: String,
    override var isOn: Boolean = false,
    val url: String,
    val cmd: CmdInterface
): ListItem