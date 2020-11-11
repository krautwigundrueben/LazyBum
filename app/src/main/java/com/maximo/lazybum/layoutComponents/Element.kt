package com.maximo.lazybum.layoutComponents

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment

interface Element {
    val text: String

    fun getViewType(): Int
    fun getView(
        convertView: View?,
        mCtx: Context,
        fragment: Fragment
    ): View
}