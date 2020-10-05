package com.maximo.lazybum

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.maximo.lazybum.Devices.arduinoApi.Device

class MyListAdapter(var mCtx: Context, var items: MutableList<ListItem>) : BaseAdapter() {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val layoutInflater:LayoutInflater = LayoutInflater.from(mCtx)
        val mItem: ListItem = items[position]
        val view: View
        val titleTextView:TextView

        if (!mItem.isSectionHeader) {
            view = layoutInflater.inflate(R.layout.row, null)

            val imageView:ImageView = view.findViewById(R.id.imageView)
            titleTextView = view.findViewById(R.id.textTitle)
            val locationTextView:TextView = view.findViewById(R.id.textLocation)

            mItem as Device
            imageView.setImageDrawable(ContextCompat.getDrawable(mCtx, mItem.img))
            imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorOff),
                PorterDuff.Mode.SRC_IN)
            titleTextView.text = mItem.title
            locationTextView.text = mItem.command.description
        } else {
            view = layoutInflater.inflate(R.layout.section_header, null)

            mItem as SectionHeader
            titleTextView = view.findViewById(R.id.textSectionHeader)
            titleTextView.text = mItem.title
        }
        return view
    }
}