package com.maximo.lazybum.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.maximo.lazybum.Globals.avReceiverFragmentGroups
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.R
import com.maximo.lazybum.R.string.avReceiverDeviceName
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.statusClasses.AvReceiverStatus
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import com.maximo.lazybum.layoutComponents.Action
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener
import kotlinx.android.synthetic.main.list_croller.*
import kotlinx.android.synthetic.main.list_croller.view.*

class AvReceiverFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.list_croller, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(), avReceiverFragmentGroups, this)

        val initAction = Action(getString(R.string.status_request_command), resources.getString(avReceiverDeviceName))
        setupVolumeKnob(view, initAction)
        addObserver(deviceManager.getDevice(initAction.deviceName))

        return view
    }

    private fun setVolumeKnobView(avReceiverStatus: AvReceiverStatus) {
        val isOn = avReceiverStatus.isActive
        val vol = avReceiverStatus.vol

        if (isOn && vol > 0) {
            volumeText.text = vol.toString().trimStart('0')
            croller.progress = vol - 40
            croller.indicatorColor = resources.getColor(R.color.colorAccent, context?.theme)
        } else {
            volumeText.text = resources.getString(R.string.text_croller)
            croller.progress = 0
            croller.indicatorColor = View.GONE
        }
    }

    private fun setupVolumeKnob(view: View, action: Action) {
        val croller: Croller = view.croller

        val onChangeListener = object: OnCrollerChangeListener {
            override fun onProgressChanged(croller: Croller?, progress: Int) {
                try {
                    if (deviceManager.getDevice(action.deviceName)?.getStatus()?.value?.isActive!!)
                        view.volumeText.text = (progress + 40).toString()
                } catch (exception: Exception) {
                    view.volumeText.text = resources.getString(R.string.text_croller)
                }
            }

            override fun onStartTrackingTouch(croller: Croller?) {}

            override fun onStopTrackingTouch(croller: Croller?) {
/*
                volume of AV Receiver has to be calculated according the boundary values:
                value in dB | description   | receiver annotation   | progress of seekbar
                -60dB       | silent        | 040VL                 | 0
                0dB         | loud!         | 160VL                 | 120
*/
                try {
                    val volString = (croller?.progress!! + 40).toString().padStart(3, '0') + "VL"
                    action.commandName = "{\"turn\":\"$volString\"}"

                    context?.let { deviceManager.launchAction(it, action) }

                } catch (e: java.lang.Exception) {
                    Toast.makeText(context, getString(R.string.croller_stop_touch_problem), Toast.LENGTH_LONG).show()
                }
            }
        }

        croller.setOnCrollerChangeListener(onChangeListener)
    }

    private fun addObserver(avReceiver: DeviceManager.DeviceViewModel?) {
        avReceiver?.getStatus()?.observe(viewLifecycleOwner,
            { t ->
                setVolumeKnobView(t as AvReceiverStatus)
                Log.e("AvReceiverFragment", "Neuer Status: isActive: ${t.isActive}, mode: ${t.mode}, vol: ${t.vol}")
            })
    }
}