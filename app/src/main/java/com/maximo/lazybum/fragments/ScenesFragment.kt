package com.maximo.lazybum.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.maximo.lazybum.Globals.scenesGroups
import com.maximo.lazybum.R
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import kotlinx.android.synthetic.main.list.view.*

class ScenesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.list, container, false)
        val listView = view.list

        listView.adapter = MyListAdapter(requireContext(), scenesGroups,this)

        return view
    }
}