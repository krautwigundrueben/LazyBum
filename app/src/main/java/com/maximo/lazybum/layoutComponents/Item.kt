package com.maximo.lazybum.layoutComponents

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.wifi.WifiManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.jem.rubberpicker.RubberSeekBar
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.Globals.supportedWifiSSIDs
import com.maximo.lazybum.R
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.DeviceManager.DeviceType.AV_RECEIVER
import com.maximo.lazybum.deviceComponents.DeviceManager.DeviceType.DIMMER
import com.maximo.lazybum.deviceComponents.deviceClasses.MyStromDimmer
import com.maximo.lazybum.deviceComponents.deviceClasses.ShellyDimmer
import com.maximo.lazybum.deviceComponents.statusClasses.AvReceiverStatus
import com.maximo.lazybum.deviceComponents.statusClasses.DimmerStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import com.maximo.lazybum.layoutComponents.Item.ItemType.*
import kotlinx.android.synthetic.main.brightness_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class Item (
    override val text: String,
    val subText: String,
    val icon: String,
    val actions: List<Action>
) : ListElement {

    private lateinit var imageView: ImageView
    private var itemType: ItemType

    enum class ItemType {
        SINGLE, SCENE, AV_REC_COMMAND, SHUTTER
    }

    init {
        if (actions.size == 1) {
            itemType = with (actions[0].deviceName) {
                when {
                    contains("AvReceiver") -> AV_REC_COMMAND
                    contains("shutter") -> SHUTTER
                    else -> SINGLE
                }
            }
        } else itemType = SCENE
    }

    override fun getView(convertView: View?, mCtx: Context, fragment: Fragment): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        var view = convertView

        if (view != null) {
            imageView = view.findViewById(R.id.imageView)

            initializeItemView(view, mCtx)
            setOnClickListener(view, mCtx)
            setOnLongClickListener(view, mCtx)
            addObservers(fragment, mCtx)
        } else {
            view = layoutInflater.inflate(R.layout.list_item, null)
            imageView = view.findViewById(R.id.imageView)

            initializeItemView(view, mCtx)
            setOnClickListener(view, mCtx)
            setOnLongClickListener(view, mCtx)
            addObservers(fragment, mCtx)
        }

        return view as View
    }

    override fun getViewType(): Int {
        return MyListAdapter.ElementType.ITEM.ordinal
    }

    private fun initializeItemView(view: View, mCtx: Context) {

        val textView: TextView = view.findViewById(R.id.titleText)
        textView.setText(text)

        val subTextView: TextView = view.findViewById(R.id.subText)
        subTextView.setText(subText)

        val imageId =
            mCtx.resources.getIdentifier(icon, "drawable", mCtx.packageName)
        imageView.setImageDrawable(ContextCompat.getDrawable(mCtx, imageId))
    }

    private fun paintIcon(mCtx: Context) {

        val allItemStatuses: MutableList<LiveData<Status>> = mutableListOf()

        for (action in actions) {
            allItemStatuses.add(deviceManager.getDevice(action.deviceName)?.getStatus()!!)
        }

        if (isItemToBePainted(allItemStatuses)) {
            imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorAccent),
                PorterDuff.Mode.SRC_IN)
        } else {
            imageView.setColorFilter(ContextCompat.getColor(mCtx, R.color.colorOff),
                PorterDuff.Mode.SRC_IN)
        }
    }

    private fun isItemToBePainted(allItemStatuses: MutableList<LiveData<Status>>): Boolean {

        when (itemType) {
            SINGLE -> return allItemStatuses[0].value?.isActive!!
            AV_REC_COMMAND -> return actions[0].commandName.contains((allItemStatuses[0].value as AvReceiverStatus).mode) && allItemStatuses[0].value?.isActive!!
            SHUTTER -> return false
            SCENE -> {
                try {
                    actions.forEachIndexed { index, action ->
                        if (action.commandName.contains("toggle") && !deviceManager.getDevice(action.deviceName)
                                ?.getStatus()?.value?.isActive!! ||
                            action.commandName.contains("on") && !allItemStatuses[index].value?.isActive!! ||
                            action.commandName.contains("off") && allItemStatuses[index].value?.isActive!!
                        ) {
                            return false
                        } else {
                            when (deviceManager.getDevice(action.deviceName)?.deviceType) {
                                DIMMER -> if (!action.commandName.contains((allItemStatuses[index].value as DimmerStatus).value)) return false
                                AV_RECEIVER -> if (!action.commandName.contains((allItemStatuses[index].value as AvReceiverStatus).mode)) return false
                                DeviceManager.DeviceType.SHUTTER -> return false
                                else -> { }
                            }
                        }
                    }
                } catch (nullPointerException: NullPointerException) {
                    return false
                }
            }
        }
        return true
    }

    private suspend fun callDeviceActions(actionList: List<Action>, context: Context) {

        val connMgr = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (supportedWifiSSIDs.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {

            actionList.forEachIndexed { index, action ->
                try {
                    deviceManager.executeCommand(actionList[index])
                } catch (e: Exception) {
                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.not_at_home), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setOnClickListener(view: View, mCtx: Context) {
        view.setOnClickListener {
            GlobalScope.launch {
                callDeviceActions(actions, mCtx)
            }
        }
    }

    private fun setOnLongClickListener(view: View, mCtx: Context) {
        with (actions[0].deviceName) {
            when {
                contains("ledGrid") -> setOnLongClickListenerGrid(view, mCtx, actions[0])
                contains("spotLight") -> setOnLongClickListenerSpots(view, mCtx, actions[0])
            }
        }
    }

    private fun setOnLongClickListenerGrid(view: View?, mCtx: Context, action: Action) {

        view?.setOnLongClickListener {

            val ledGrid = deviceManager.getDevice(action.deviceName)?.dInstance as MyStromDimmer
            val rgb = ledGrid.responseObj.color.substring(2, 8)
            val ww = ledGrid.responseObj.color.substring(0, 2)

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

                    GlobalScope.launch {
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

    private fun setOnLongClickListenerSpots(view: View?, mCtx: Context, action: Action) {

        view?.setOnLongClickListener {

            val spots = deviceManager.getDevice(action.deviceName)?.dInstance as ShellyDimmer
            var spotsBrightness = spots.responseObj.brightness

            val mDialogView =
                LayoutInflater.from(mCtx).inflate(R.layout.brightness_dialog, null)
            val rubberSeekBar = mDialogView.rubberSeekBar

            AlertDialog.Builder(mCtx)
                .setView(mDialogView)
                .setTitle("Helligkeit wählen")
                .setPositiveButton("Ok") { diaglog, selectedBrightness -> }
                .show()

            rubberSeekBar.setCurrentValue(spotsBrightness)
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

                    GlobalScope.launch {
                        callDeviceActions(actns, mCtx)
                    }
                }
            })
            true
        }
    }

    private fun addObservers(fragment: Fragment, mCtx: Context) {
        for (action in actions) {
            val device = deviceManager.getDevice(action.deviceName)

            device?.getStatus()?.observe(fragment, object: Observer<Status> {
                override fun onChanged(status: Status?) {
                    paintIcon(mCtx)
                    Log.e("Item", "Der neue Status von ${device.dName} ist ${status?.isActive}")
                }
            })
        }
    }
}
