package com.maximo.lazybum.layoutAdapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import com.maximo.lazybum.layoutComponents.Element
import com.maximo.lazybum.layoutComponents.Group
import com.maximo.lazybum.layoutComponents.Header
import com.maximo.lazybum.layoutComponents.Item

class MyListAdapter(private var context: Context, groups: List<Group>, private val fragment: Fragment) : BaseAdapter() {

    private val listItems = mutableListOf<Element>()

    init {
        for (group in groups) {
            registerGroupHeader(group.group_name)
            registerGroupItems(group.items)
        }
    }

    private fun registerGroupHeader(groupName: String) {
        val instance = Header(groupName)
        listItems.add(instance)
    }

    private fun registerGroupItems(groupItems: List<Item>) {
        groupItems.forEach { item ->
            val instance = Item(item.text, item.subText, item.icon, item.actions)
            listItems.add(instance)
        }
    }

    enum class ElementType {
        HEADER, ITEM
    }

    override fun getViewTypeCount(): Int {
        return ElementType.values().size
    }

    override fun getItemViewType(position: Int): Int {
        return listItems[position].getViewType()
    }

    override fun getCount(): Int {
        return listItems.size
    }

    override fun getItem(position: Int): Any {
        return listItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        var view = convertView
        view = listItems[position].getView(view, context, fragment)

        return view
    }
}