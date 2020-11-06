package com.maximo.lazybum.layoutAdapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.maximo.lazybum.layoutComponents.Group
import com.maximo.lazybum.layoutComponents.Header
import com.maximo.lazybum.layoutComponents.Item
import com.maximo.lazybum.layoutComponents.ListElement

// info: https://stackoverflow.com/questions/13590627/android-listview-headers

class MyListAdapter(var mCtx: Context, groups: List<Group>) : BaseAdapter() {

    val listItems = mutableListOf<ListElement>()

    // TODO: OnLongClickListener hinzufügen

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

    fun registerGroupItems(groupItems: List<Item>) {
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
        view = listItems[position].getView(view, mCtx)

        return view
    }
}

/*

        listView.setOnItemLongClickListener { parent, v, position, id ->
            if (!btnListView[position].isHeader) {
                val clickedDevice = btnListView[position] as ListAction
                when (clickedDevice.id.toInt()) {
                    5 ->
                    6 -> {
                        val mDialogView =
                            LayoutInflater.from(activity).inflate(R.layout.brightness_dialog, null)
                        val rubberSeekBar = mDialogView.rubberSeekBar

                        AlertDialog.Builder(activity)
                            .setView(mDialogView)
                            .setTitle("Helligkeit wählen")
                            .setPositiveButton("Ok") { diaglog, selectedBrightness -> }
                            .show()

                        rubberSeekBar.setCurrentValue(spotBrightness)
                        rubberSeekBar.setOnRubberSeekBarChangeListener(object :
                            RubberSeekBar.OnRubberSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: RubberSeekBar,
                                value: Int,
                                fromUser: Boolean,
                            ) {
                                spotBrightness = value
                            }

                            override fun onStartTrackingTouch(seekBar: RubberSeekBar) {}

                            override fun onStopTrackingTouch(seekBar: RubberSeekBar) {
                                if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                                    execute(btnListView[position] as ListAction,
                                        ShellyDimmerCommand("on", spotBrightness.toString()),
                                        listView)
                                } else {
                                    Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        })
                    }
                }
            }
            true
        }
 */