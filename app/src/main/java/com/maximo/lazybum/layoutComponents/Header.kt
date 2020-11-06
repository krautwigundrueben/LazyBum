package com.maximo.lazybum.layoutComponents

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.maximo.lazybum.R
import com.maximo.lazybum.layoutAdapter.MyListAdapter

data class Header(
    override val text: String
): ListElement {

    override fun getView(
        convertView: View?,
        mCtx: Context
    ): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        var view = convertView

        if (view == null) {
            view = layoutInflater.inflate(R.layout.list_group_header, null)
            val textView: TextView = view.findViewById(R.id.groupHeaderText)
            textView.setText(text)
        } else {
            view = convertView
        }
        return view as View
    }

    override fun getViewType(): Int {
        return MyListAdapter.ElementType.HEADER.ordinal
    }
}