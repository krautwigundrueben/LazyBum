package com.maximo.lazybum

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_smart_home.view.*

class SmartHomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val deviceList = mutableListOf<Device>()

        deviceList.add(
            Device(
                0,
                "Kaffeemaschine",
                "Küche",
                R.drawable.ic_coffee,
                "http://192.168.178.34",
                false,
                Command("","","","",0)
            )
        )
        deviceList.add(
            Device(
                1,
                "LED Grid",
                "Wohnzimmer",
                R.drawable.ic_led_grid,
                "http://192.168.178.32",
                false,
                Command("toggle", "", "33000000", "rgb", 2000)
            )
        )
        deviceList.add(
            Device(
                2,
                "Strahler",
                "Wohnzimmer",
                R.drawable.ic_spots,
                "http://192.168.178.45",
                false,
                Command("toggle", "40", "", "", 0)
            ))
        deviceList.add(
            Device(
                3,
                "Esstischlampe",
                "Essbereich",
                R.drawable.ic_dining,
                "http://192.168.178.46",
                false,
                Command("toggle", "", "", "", 0)
            )
        )

        val view = inflater.inflate(R.layout.fragment_smart_home, container, false)
        val listView = view.smart_home_list
        listView.adapter = MyListAdapter(requireContext(), R.layout.row, deviceList)

        return view
    }
}