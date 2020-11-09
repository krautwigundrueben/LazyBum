package com.maximo.lazybum.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.maximo.lazybum.Globals.DEVICES_TAB_POS
import com.maximo.lazybum.Globals.devicesFragmentGroups
import com.maximo.lazybum.Globals.myListAdapters
import com.maximo.lazybum.R
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import kotlinx.android.synthetic.main.list.view.*


class DevicesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.list, container, false)
        val listView = view.list
        listView.adapter = MyListAdapter(requireContext(), devicesFragmentGroups, this)

        if (!myListAdapters.containsKey(DEVICES_TAB_POS))
            myListAdapters.put(DEVICES_TAB_POS, listView.adapter as MyListAdapter)
        else myListAdapters.replace(DEVICES_TAB_POS, listView.adapter as MyListAdapter)

        return view
    }
}