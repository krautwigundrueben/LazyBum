package com.maximo.lazybum.uiComponents

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.maximo.lazybum.R

class MyListAdapter(var mCtx: Context, var rows: MutableList<ListRow>) : BaseAdapter() {

    override fun getCount(): Int {
        return rows.size
    }

    override fun getItem(position: Int): Any {
        return rows[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        val mItem: ListRow = rows[position]
        val view: View
        val titleTextView: TextView

        if (!mItem.isHeader) {
            view = layoutInflater.inflate(R.layout.list_item, null)

            val imageView:ImageView = view.findViewById(R.id.imageView)
            titleTextView = view.findViewById(R.id.textTitle)
            val locationTextView:TextView = view.findViewById(R.id.textLocation)

            mItem as ListItem
            imageView.setImageDrawable(ContextCompat.getDrawable(mCtx, mItem.img))
            if (mItem.isOn) {
                imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorAccent),
                    PorterDuff.Mode.SRC_IN)
            }
            else {
                imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorOff),
                    PorterDuff.Mode.SRC_IN)
            }
            titleTextView.text = mItem.text
            locationTextView.text = mItem.description
        }
        else {
            view = layoutInflater.inflate(R.layout.list_section_header, null)

            mItem as ListSectionHeader
            titleTextView = view.findViewById(R.id.textSectionHeader)
            titleTextView.text = mItem.text
        }

        return view
    }
}