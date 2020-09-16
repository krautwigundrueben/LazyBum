package com.maximo.lazybum

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.maximo.lazybum.Devices.arduinoApi.Device

class MyListAdapter(var mCtx: Context, var resources: Int, var items: MutableList<Device>):ArrayAdapter<Device>(mCtx, resources, items) {

    private var TAG = "ListAdapter"

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater:LayoutInflater = LayoutInflater.from(mCtx)
        val view:View = layoutInflater.inflate(resources, null)

        val imageView:ImageView = view.findViewById(R.id.imageView)
        val titleTextView:TextView = view.findViewById(R.id.textTitle)
        val locationTextView:TextView = view.findViewById(R.id.textLocation)

        val mItem: Device = items[position]
        imageView.setImageDrawable(ContextCompat.getDrawable(mCtx, mItem.img))
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorOff), PorterDuff.Mode.SRC_IN)
        titleTextView.text = mItem.title
        locationTextView.text = mItem.location

        return view
    }
}