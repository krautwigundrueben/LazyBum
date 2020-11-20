package com.maximo.lazybum.layoutComponents

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.jem.rubberpicker.RubberSeekBar
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.R
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.DeviceManager.DeviceType.AV_RECEIVER
import com.maximo.lazybum.deviceComponents.DeviceManager.DeviceType.DIMMER
import com.maximo.lazybum.deviceComponents.deviceClasses.MyStromDimmer
import com.maximo.lazybum.deviceComponents.deviceClasses.ShellyDimmer
import com.maximo.lazybum.deviceComponents.statusClasses.AvReceiverStatus
import com.maximo.lazybum.deviceComponents.statusClasses.DimmerStatus
import com.maximo.lazybum.deviceComponents.statusClasses.ShutterStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import com.maximo.lazybum.layoutComponents.Item.ItemType.*
import kotlinx.android.synthetic.main.brightness_dialog.view.*

data class Item (
    override val mainText: String,
    val subText: String,
    val icon: String,
    val actionList: List<Action>
) : Element {

    private lateinit var imageView: ImageView
    private lateinit var subtextView: TextView
    private var itemType: ItemType

    enum class ItemType {
        SINGLE, SCENE, AV_REC_COMMAND, SHUTTER
    }

    init {
        itemType = if (actionList.size == 1) {
            with (actionList[0].deviceName) {
                when {
                    contains("AvReceiver") -> AV_REC_COMMAND
                    contains("shutter") -> SHUTTER
                    else -> SINGLE
                }
            }
        } else SCENE
    }

    @SuppressLint("InflateParams")
    override fun getView(convertView: View?, context: Context, fragment: Fragment): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        var view = convertView

        if (view == null) {
            view = layoutInflater.inflate(R.layout.list_item, null)
        }

        imageView = view?.findViewById(R.id.imageView)!!
        subtextView = view.findViewById(R.id.subText)

        initializeItemView(view, context)
        setOnClickListener(view, context)
        setOnLongClickListener(view, context)
        addObservers(fragment, context)

        return view
    }

    override fun getViewType(): Int {
        return MyListAdapter.ElementType.ITEM.ordinal
    }

    private fun initializeItemView(view: View, context: Context) {

        val textView: TextView = view.findViewById(R.id.titleText)
        textView.text = mainText

        val subTextView: TextView = view.findViewById(R.id.subText)
        subTextView.text = subText

        val imageId =
            context.resources.getIdentifier(icon, "drawable", context.packageName)
        imageView.setImageDrawable(ContextCompat.getDrawable(context, imageId))
    }

    private fun paintStatus(context: Context) {

        val allItemStatuses: MutableList<LiveData<Status>> = mutableListOf()

        for (action in actionList) {
            allItemStatuses.add(deviceManager.getDevice(action.deviceName)?.getStatus()!!)
        }

        accentuateIcon(allItemStatuses, context)

        if (itemType == SHUTTER) {
            accentuateSubTextSpan((allItemStatuses[0].value as ShutterStatus).nextGo)
        } else if (itemType == SINGLE)
            if (allItemStatuses[0].value?.isActive!!) accentuateSubTextSpan("aus")
            else accentuateSubTextSpan("an")
    }

    private fun accentuateSubTextSpan(spanToBeAccentuated: String) {
        val startIndex = subText.indexOf(spanToBeAccentuated)
        val endIndex =
            if (subText.indexOf(" ", startIndex) > 0) subText.indexOf(" ", startIndex)
            else subText.lastIndex + 1

        val accentuatedSubText = SpannableString(subText)
        try {
            accentuatedSubText.setSpan(UnderlineSpan(), startIndex, endIndex, 0)
            subtextView.text = accentuatedSubText
        } catch (e: Exception) { }
    }

    private fun accentuateIcon(allItemStatuses: MutableList<LiveData<Status>>, context: Context) {
        if (isIconToBeAccentuated(allItemStatuses)) {
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent),
                PorterDuff.Mode.SRC_IN)
        } else {
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorOff),
                PorterDuff.Mode.SRC_IN)
        }
    }

    private fun isIconToBeAccentuated(allItemStatuses: MutableList<LiveData<Status>>): Boolean {

        when (itemType) {
            SINGLE -> return allItemStatuses[0].value?.isActive!!
            AV_REC_COMMAND -> return actionList[0].commandName.contains((allItemStatuses[0].value as AvReceiverStatus).mode) && allItemStatuses[0].value?.isActive!!
            SHUTTER -> return false
            SCENE -> {
                try {
                    actionList.forEachIndexed { index, action ->
                        if (action.commandName.contains("toggle") &&
                            !deviceManager.getDevice(action.deviceName)?.getStatus()?.value?.isActive!! ||
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

    private fun setOnClickListener(view: View, context: Context) {
        view.setOnClickListener {
            for (action in actionList) {
                deviceManager.launchAction(context, action)
            }
        }
    }

    private fun setOnLongClickListener(view: View, context: Context) {

        with(mainText) {
            when {
                contains("Grid") -> setOnLongClickListenerGrid(view, context)
                contains("Strahler") -> setOnLongClickListenerSpots(view, context)
                contains("Spotify") -> setOnLongClickListenerSpotify(view, context)
            }
        }
    }

    private fun setOnLongClickListenerSpotify(view: View, context: Context) {

        view.setOnLongClickListener {

            for (action in actionList) {
                deviceManager.launchAction(context, action)
            }

            val packageManager = context.packageManager
            try {
                val intent = packageManager.getLaunchIntentForPackage(context.getString(R.string.open_spotify_package_name))
                intent?.addCategory(Intent.CATEGORY_LAUNCHER)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.open_spotify_not_installed), Toast.LENGTH_LONG).show()
            }

            true
        }
    }

    private fun setOnLongClickListenerGrid(view: View, context: Context) {

        view.setOnLongClickListener {

            val ledGrid = deviceManager.getDevice(actionList[0].deviceName)?.dInstance as MyStromDimmer
            val rgb = if (ledGrid.isResponseInitialized()) ledGrid.responseObj.color.substring(2, 8)
            else context.getString(R.string.init_color_grid_picker_1)

            val ww = if (ledGrid.isResponseInitialized()) ledGrid.responseObj.color.substring(0, 2)
            else context.getString(R.string.init_color_grid_picker_2)

            var gridColor: Int

            gridColor = if (rgb != context.getString(R.string.black)) Color.parseColor("#$rgb")
            else Color.parseColor("#$ww$ww$ww")

            ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.grid_picker_choose_color_text))
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .initialColor(gridColor)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorChangedListener { selectedColor ->
                    var color = "00" + Integer.toHexString(selectedColor).takeLast(6)
                    if (color.regionMatches(2, color, 4, 2) && color.regionMatches(
                            2, color, 6, 2)) {
                        color = color.substring(2, 4) + context.getString(R.string.black)
                    }

                    deviceManager.launchAction(context,
                        Action(actionList[0].deviceName, "{\"color\":\"$color\",\"mode\":\"rgb\",\"action\":\"on\",\"ramp\":\"0\"}"))
                }
                .setPositiveButton(context.getString(R.string.Ok)) { dialog, selectedColor, allColors ->
                    gridColor = selectedColor
                }
                .build()
                .show()
            true
        }
    }

    private fun setOnLongClickListenerSpots(view: View?, context: Context) {

        view?.setOnLongClickListener {

            val middle = 50
            val spots = deviceManager.getDevice(actionList[0].deviceName)?.dInstance as ShellyDimmer
            var spotsBrightness = if (spots.isResponseInitialized()) spots.responseObj.brightness
            else middle

            val mDialogView =
                LayoutInflater.from(context).inflate(R.layout.brightness_dialog, null)
            val rubberSeekBar = mDialogView.rubberSeekBar

            AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle(context.getString(R.string.spots_picker_choose_brightness))
                .setPositiveButton(context.getString(R.string.Ok)) { diaglog, selectedBrightness -> }
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

                    deviceManager.launchAction(context,
                        Action(actionList[0].deviceName, "{\"turn\":\"on\",\"brightness\":\"${spotsBrightness}\"}"))
                }
            })
            true
        }
    }

    private fun addObservers(fragment: Fragment, context: Context) {
        for (action in actionList) {
            val device = deviceManager.getDevice(action.deviceName)

            device?.getStatus()?.observe(fragment,
                { status ->
                    paintStatus(context)
                    Log.e("Item", "Der neue Status von ${device.dName} ist ${status?.isActive}")
                })
        }
    }
}
