package com.maximo.lazybum.layoutComponents

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.SpannableString
import android.text.style.UnderlineSpan
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
    val mainText: String,
    val subText: String,
    val icon: String,
    val actions: List<Action>
) : Element {

    private lateinit var imageView: ImageView
    private lateinit var subtextView: TextView
    private var itemType: ItemType

    enum class ItemType {
        SINGLE, SCENE, AV_REC_COMMAND, SHUTTER, VACUUM
    }

    init {
        itemType = if (actions.size == 1) {
            with (actions[0].deviceName) {
                when {
                    contains("AvReceiver") -> AV_REC_COMMAND
                    contains("shutter") -> SHUTTER
                    contains("roborock") -> VACUUM
                    else -> SINGLE
                }
            }
        } else SCENE
    }

    @SuppressLint("InflateParams")
    override fun getView(convertView: View?, mCtx: Context, fragment: Fragment): View {

        val layoutInflater: LayoutInflater = LayoutInflater.from(mCtx)
        var view = convertView

        if (view == null) {
            view = layoutInflater.inflate(R.layout.list_item, null)
        }

        imageView = view?.findViewById(R.id.imageView)!!
        subtextView = view.findViewById(R.id.subText)

        initializeItemView(view, mCtx)
        setOnClickListener(view, mCtx)
        setOnLongClickListener(view, mCtx)
        addObservers(fragment, mCtx)

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

        for (action in actions) {
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
            AV_REC_COMMAND -> return actions[0].commandName.contains((allItemStatuses[0].value as AvReceiverStatus).mode) && allItemStatuses[0].value?.isActive!!
            SHUTTER -> return false
            VACUUM -> return false
            SCENE -> {
                try {
                    actions.forEachIndexed { index, action ->
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
            for (action in actions) {
                deviceManager.launchAction(context, action)
            }
        }
    }

    private fun setOnLongClickListener(view: View, context: Context) {

        with(mainText) {
            when {
                contains("Grid") || contains("Zimmerlampe") -> setOnLongClickListenerGrid(view, context)
                contains("Strahler") -> setOnLongClickListenerSpots(view, context)
                contains("Spotify") -> setOnLongClickListenerSpotify(view, context)
            }
        }
    }

    private fun setOnLongClickListenerSpotify(view: View, context: Context) {

        view.setOnLongClickListener {

            for (action in actions) {
                deviceManager.launchAction(context, action)
            }

            val packageManager = context.packageManager
            try {
                val intent: Intent? = packageManager.getLaunchIntentForPackage(context.getString(R.string.package_name_spotify))
                intent?.addCategory(CATEGORY_APP_MUSIC)
                intent?.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_REQUIRE_NON_BROWSER or FLAG_ACTIVITY_REQUIRE_DEFAULT
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.error_spotify_not_installed), Toast.LENGTH_LONG).show()
            }

            true
        }
    }

    private fun setOnLongClickListenerGrid(view: View, context: Context) {

        view.setOnLongClickListener {

            var currentColor = getCurrentColor(context)

            ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.text_choose_color))
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .initialColor(currentColor)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorChangedListener { selectedColor ->
                    var color = "00" + Integer.toHexString(selectedColor).takeLast(6)
                    if (color.regionMatches(2, color, 4, 2) && color.regionMatches(
                            2, color, 6, 2)) {
                        color = color.substring(2, 4) + context.getString(R.string.black)
                    }

                    deviceManager.launchAction(context,
                        Action(actions[0].deviceName, "{\"color\":\"$color\",\"mode\":\"rgb\",\"action\":\"on\",\"ramp\":\"0\"}"))
                }
                .setPositiveButton(context.getString(R.string.text_ok)) { dialog, selectedColor, allColors ->
                    currentColor = selectedColor
                }
                .build()
                .show()
            true
        }
    }

    private fun getCurrentColor(context: Context): Int {

        val light = deviceManager.getDevice(actions[0].deviceName)?.dInstance as MyStromDimmer
        val rgb: String
        val ww: String

        if (light.isResponseInitialized() && !light.responseObj.color.contains(';')) {
            rgb = light.responseObj.color.substring(2, 8)
            ww = light.responseObj.color.substring(0, 2)
        } else {
            rgb = context.getString(R.string.color_picker_init_1)
            ww = context.getString(R.string.color_picker_init_2)
        }

        return if (rgb != context.getString(R.string.black)) Color.parseColor("#$rgb")
        else Color.parseColor("#$ww$ww$ww")
    }

    private fun setOnLongClickListenerSpots(view: View?, context: Context) {

        view?.setOnLongClickListener {

            val middle = 50
            val spots = deviceManager.getDevice(actions[0].deviceName)?.dInstance as ShellyDimmer
            var spotsBrightness = if (spots.isResponseInitialized()) spots.responseObj.brightness
            else middle

            val mDialogView =
                LayoutInflater.from(context).inflate(R.layout.brightness_dialog, null)
            val rubberSeekBar = mDialogView.rubberSeekBar

            AlertDialog.Builder(context)
                .setView(mDialogView)
                .setTitle(context.getString(R.string.text_choose_brightness))
                .setPositiveButton(context.getString(R.string.text_ok)) { diaglog, selectedBrightness -> }
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
                        Action(actions[0].deviceName, "{\"turn\":\"on\",\"brightness\":\"${spotsBrightness}\"}"))
                }
            })
            true
        }
    }

    private fun addObservers(fragment: Fragment, context: Context) {
        for (action in actions) {
            val device = deviceManager.getDevice(action.deviceName)

            device?.getStatus()?.observe(fragment,
                { status ->
                    paintStatus(context)
                    // Log.e("Item", "Der neue Status von ${device.dName} ist ${status?.isActive}")
                })
        }
    }
}
