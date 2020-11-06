package com.maximo.lazybum.layoutComponents

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.jem.rubberpicker.RubberSeekBar
import com.maximo.lazybum.Globals.globalDeviceManager
import com.maximo.lazybum.Globals.myListAdapters
import com.maximo.lazybum.Globals.supportedWifiSsids
import com.maximo.lazybum.R
import com.maximo.lazybum.deviceComponents.deviceClasses.MyStromDimmer
import com.maximo.lazybum.deviceComponents.deviceClasses.ShellyDimmer
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import kotlinx.android.synthetic.main.brightness_dialog.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class Item (
    override val text: String,
    val subText: String,
    val icon: String,
    val actions: List<Action>,
) : ListElement {

    override fun getView(convertView: View?, mCtx: Context): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        var view = convertView

        if (view != null) {
            view = convertView
        }
        else {
            view = layoutInflater.inflate(R.layout.list_item, null)

            initializeItemView(view, mCtx)
            setOnClickListener(view, mCtx)
            getInitialStatus(mCtx)

            if (actions[0].deviceName == "ledGridSwitch") {
                setOnLongClickListenerGrid(view, mCtx, actions[0])
            }

            if (actions[0].deviceName == "spotLight") {
                setOnLongClickListenerSpots(view, mCtx, actions[0])
            }
        }

        paintIcon(view!!.findViewById(R.id.imageView), mCtx)

        return view
    }

    private fun setOnLongClickListenerSpots(view: View?, mCtx: Context, action: Action) {

        view?.setOnLongClickListener {

            val spots = globalDeviceManager.getDevice(action.deviceName)?.dInstance as ShellyDimmer
            var spotsBrightness = spots.deviceStatus?.brightness

            val mDialogView =
                LayoutInflater.from(mCtx).inflate(R.layout.brightness_dialog, null)
            val rubberSeekBar = mDialogView.rubberSeekBar

            AlertDialog.Builder(mCtx)
                .setView(mDialogView)
                .setTitle("Helligkeit wählen")
                .setPositiveButton("Ok") { diaglog, selectedBrightness -> }
                .show()

            rubberSeekBar.setCurrentValue(spotsBrightness as Int)
            rubberSeekBar.setOnRubberSeekBarChangeListener(object :
                RubberSeekBar.OnRubberSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: RubberSeekBar,
                    value: Int,
                    fromUser: Boolean,
                ) {
                    spotsBrightness = value
                }

                override fun onStartTrackingTouch(seekBar: RubberSeekBar) {}

                override fun onStopTrackingTouch(seekBar: RubberSeekBar) {
                    val actns: MutableList<Action> = mutableListOf(
                        Action("{\"turn\":\"on\",\"brightness\":\"${spotsBrightness.toString()}\"}", action.deviceName)
                    )

                    GlobalScope.launch(Main) {
                        callDeviceActions(actns, mCtx)
                    }
                }
            })
            true
        }
    }

    private fun setOnLongClickListenerGrid(view: View?, mCtx: Context, action: Action) {

        view?.setOnLongClickListener {

            val ledGrid = globalDeviceManager.getDevice(action.deviceName)?.dInstance as MyStromDimmer
            val rgb = ledGrid.deviceStatus?.color?.substring(2, 8)
            val ww = ledGrid.deviceStatus?.color?.substring(0, 2)

            var gridColor: Int

            if (rgb != "000000") {
                gridColor = Color.parseColor("#" + rgb)
            } else {
                gridColor = Color.parseColor("#" + ww + ww + ww)
            }

            ColorPickerDialogBuilder
                .with(mCtx)
                .setTitle("Farbe wählen")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .initialColor(gridColor)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorChangedListener { selectedColor ->
                    var color = "00" + Integer.toHexString(selectedColor).takeLast(6)
                    if (color.regionMatches(2, color, 4, 2) && color.regionMatches(
                            2, color,6,2))
                        color = color.substring(2, 4) + "000000"

                    val actns: MutableList<Action> = mutableListOf(
                        Action("{\"color\":\"$color\",\"mode\":\"rgb\",\"action\":\"on\",\"ramp\":\"0\"}", action.deviceName)
                    )

                    GlobalScope.launch(Main) {
                        callDeviceActions(actns, mCtx)
                    }
                }
                .setPositiveButton("Ok") { dialog, selectedColor, allColors ->
                    gridColor = selectedColor
                }
                .build()
                .show()
            true
        }
    }

    private fun getInitialStatus(mCtx: Context) {
        val statusActions: MutableList<Action> = mutableListOf()
        for (action in actions) {
            statusActions.add(Action("status", action.deviceName))
        }

        GlobalScope.launch(Main) {
            callDeviceActions(statusActions, mCtx)
        }
    }

    private fun setOnClickListener(view: View, mCtx: Context) {
        view.setOnClickListener {
            GlobalScope.launch(Main) {
                callDeviceActions(actions, mCtx)
            }
        }
    }

    private fun initializeItemView(view: View, mCtx: Context) {

        val textView: TextView = view.findViewById(R.id.titleText)
        textView.setText(text)

        val subTextView: TextView = view.findViewById(R.id.subText)
        subTextView.setText(subText)

        val imageView: ImageView = view.findViewById(R.id.imageView)
        val imageID =
            mCtx.resources.getIdentifier(icon, "drawable", mCtx.packageName)
        imageView.setImageDrawable(ContextCompat.getDrawable(mCtx, imageID))

    }

    private fun paintIcon(imageView: ImageView, mCtx: Context) {

        // TODO: zuerst nach Item Type unterscheiden: single, scene, mode, shutter?

        var inTargetState = 0
        val irregularCommandTerms = arrayOf("next", "close", "toggle")
        val regularPaintDevices = getDevicesWithoutTerms(actions, irregularCommandTerms)

        for (target in regularPaintDevices) {
            val device = globalDeviceManager.getDevice(target.deviceName)
            // val device = (Globals.viewModel as DeviceManager).devices.value?.find { it.dName == target.deviceName }

            if (target.commandName.contains(device?.dStatus!!, true)) {
                inTargetState += 1
            }
        }

        var toggledDeviceIsOn = false
        val indirectCommandTerms = arrayOf("toggle")
        val irregularPaintDevices = getDevicesWithTerms(actions, indirectCommandTerms)

        if (irregularPaintDevices.size == 1) {
            val device = globalDeviceManager.getDevice(irregularPaintDevices[0].deviceName)
            //val device = (Globals.viewModel as DeviceManager).devices.value?.find { it.dName == irregularPaintDevices[0].deviceName }

            if (device?.dStatus != "off") {
                toggledDeviceIsOn = true
            }
        }

        var modeIsSet = false
        val modeTerms = arrayOf("TV", "Bose", "CCaudio")
        val modeItems = getDevicesWithTerms(actions, modeTerms)

        for (item in modeItems) {
            val device = globalDeviceManager.getDevice(item.deviceName)
            //val device = (Globals.viewModel as DeviceManager).devices.value?.find { it.dName == item.deviceName }

            if (device?.dStatus == item.commandName) {
                modeIsSet = true
            }
        }

        if ((regularPaintDevices.size == inTargetState && regularPaintDevices.isNotEmpty()) ||
            actions.size == 1 && toggledDeviceIsOn || modeIsSet
        ) {
            imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorAccent),
                PorterDuff.Mode.SRC_IN)
        } else {
            imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorOff),
                PorterDuff.Mode.SRC_IN)
        }
    }

    private fun getDevicesWithoutTerms(actions: List<Action>, commandTerms: Array<String>): List<Action> {
        val filteredNotDeviceList: MutableList<Action> = mutableListOf()

        actions.forEachIndexed { index, action ->
            var actionContainsOneOf = false
            commandTerms.forEach { term ->
                if (actions[index].commandName.contains(term)) {
                    actionContainsOneOf = true
                }
            }
            if (!actionContainsOneOf)
                filteredNotDeviceList.add(actions[index])
        }

        return filteredNotDeviceList
    }

    private fun getDevicesWithTerms(actions: List<Action>, commandTerms: Array<String>): List<Action> {
        val filteredDeviceList: MutableList<Action> = mutableListOf()

        actions.forEachIndexed { index, action ->
            var actionContainsOneOf = false
            commandTerms.forEach { term ->
                if (actions[index].commandName.contains(term)) {
                    actionContainsOneOf = true
                }
            }
            if (actionContainsOneOf)
                filteredDeviceList.add(actions[index])
        }

        return filteredDeviceList
    }

    suspend fun callDeviceActions(actns: List<Action>, context: Context) {

        val connMgr = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {

            actns.forEachIndexed { index, action ->
                try {
                    GlobalScope.async(IO) {
                        globalDeviceManager.executeCommand(actns[index])
                        //(Globals.viewModel as DeviceManager).executeCommand(actns[index])
                    }.await()
                } catch (e: Exception) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }

            for ((_, adapter) in myListAdapters)
                adapter.notifyDataSetChanged()

        } else {
            Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getViewType(): Int {
        return MyListAdapter.ElementType.ITEM.ordinal
    }
}
