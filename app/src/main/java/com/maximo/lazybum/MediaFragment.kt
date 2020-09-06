package com.maximo.lazybum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_media.view.*

class MediaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val deviceList = mutableListOf<Device>()

        deviceList.add(
            Device(
            4,
            "Fernseher",
            "Wohnzimmer",
             R.drawable.ic_monitor,
            "http://192.168.178.43",
            false,
             Command("","","","",0)
            )
        )

        val view = inflater.inflate(R.layout.fragment_media, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(), R.layout.row, deviceList)

        return view
    }
}