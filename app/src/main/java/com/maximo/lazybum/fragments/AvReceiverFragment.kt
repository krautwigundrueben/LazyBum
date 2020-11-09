package com.maximo.lazybum.fragments

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.maximo.lazybum.Globals
import com.maximo.lazybum.Globals.avReceiverFragmentGroups
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.R
import com.maximo.lazybum.R.string.avReceiverDeviceName
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.statusClasses.AvReceiverStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import com.maximo.lazybum.layoutComponents.Action
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener
import kotlinx.android.synthetic.main.list_croller.*
import kotlinx.android.synthetic.main.list_croller.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AvReceiverFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.list_croller, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(), avReceiverFragmentGroups, this)

        val initAction = Action("status", resources.getString(avReceiverDeviceName))

        setupVolumeKnob(view, initAction)
        addObserver(deviceManager.getDevice(initAction.deviceName))

        GlobalScope.launch { callDeviceAction(initAction) }

        return view
    }

    private suspend fun callDeviceAction(action: Action) {

        val connMgr = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSSIDs.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {

            try {
                deviceManager.executeCommand(action)
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, getString(R.string.not_at_home), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setVolumeKnobView(avReceiverStatus: AvReceiverStatus) {
        val isOn = avReceiverStatus.isActive
        val vol = avReceiverStatus.vol

        if (isOn) {
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
                val volString = (croller?.progress!! + 40).toString().padStart(3, '0') + "VL"
                action.commandName = "{\"turn\":\"$volString\"}"

                GlobalScope.launch { callDeviceAction(action) }
            }
        }

        croller.setOnCrollerChangeListener(onChangeListener)
    }

    private fun addObserver(avReceiver: DeviceManager.DeviceViewModel?) {
        avReceiver?.getStatus()?.observe(viewLifecycleOwner, object: Observer<Status>{
            override fun onChanged(t: Status?) {
                setVolumeKnobView(t as AvReceiverStatus)
                Log.e("AvReceiverFragment", "Der Status hat sich geändert. isActive: ${t.isActive}, mode: ${t.mode}, vol: ${t.vol}")
            }
        })
    }
}