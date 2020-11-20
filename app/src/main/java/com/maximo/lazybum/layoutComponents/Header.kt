package com.maximo.lazybum.layoutComponents

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.maximo.lazybum.R
import com.maximo.lazybum.layoutAdapter.MyListAdapter

data class Header(
    override val mainText: String
): Element {

    @SuppressLint("InflateParams")
    override fun getView(
        convertView: View?,
        mCtx: Context,
        fragment: Fragment
    ): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        var view = convertView

        if (view != null) {
            val textView: TextView = view.findViewById(R.id.groupHeaderText)
            textView.text = mainText
        } else {
            view = layoutInflater.inflate(R.layout.list_group_header, null)
            val textView: TextView = view.findViewById(R.id.groupHeaderText)
            textView.text = mainText
        }
        return view as View
    }

    override fun getViewType(): Int {
        return MyListAdapter.ElementType.HEADER.ordinal
    }
}