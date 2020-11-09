package com.maximo.lazybum.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.maximo.lazybum.Globals
import com.maximo.lazybum.Globals.AVREC_TAB_POS
import com.maximo.lazybum.Globals.globalDeviceManager
import com.maximo.lazybum.Globals.myListAdapters
import com.maximo.lazybum.R
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.statusClasses.AvReceiverStatus
import com.maximo.lazybum.deviceComponents.statusClasses.Status
import com.maximo.lazybum.layoutAdapter.MyListAdapter
import com.maximo.lazybum.layoutComponents.Action
import com.sdsmdg.harjot.crollerTest.Croller
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener
import kotlinx.android.synthetic.main.list_croller.*
import kotlinx.android.synthetic.main.list_croller.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AvReceiverFragment : Fragment() {

    /*
    lateinit var avRecViewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState

        val deviceName = getString(R.string.avReceiverDeviceName)
        val avReceiver = globalDeviceManager.getDevice(deviceName)
        val avRecViewModel = avReceiver?.getViewModel(this)

        avRecViewModel = ViewModelProvider(this).get(ArduinoAvReceiver::class.java)
        (avRecViewModel as ArduinoAvReceiver).currentVolume.observe(this, Observer<Int> {
            volumeText.text = it.toString()
        })
    }
*/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.list_croller, container, false)
        val listView = view.media_list
        listView.adapter = MyListAdapter(requireContext(),
            Globals.avReceiverFragmentGroups, this)

        if (!myListAdapters.containsKey(AVREC_TAB_POS))
            myListAdapters.put(AVREC_TAB_POS, listView.adapter as MyListAdapter)
        else myListAdapters.replace(AVREC_TAB_POS, listView.adapter as MyListAdapter)

        val acReceiverName = getString(R.string.avReceiverDeviceName)
        val avReceiver = globalDeviceManager.getDevice(acReceiverName)
        val croller: Croller = view.croller
        val action = Action("status", acReceiverName)

        if (view != null) {
            croller.setOnCrollerChangeListener(getCrollerOnChangeListener(avReceiver, action, view))
            GlobalScope.launch { executeCommand(action, avReceiver) }
        }

        val avRecViewModel = globalDeviceManager.getDevice(acReceiverName)
        avRecViewModel?.getStatus()?.observe(viewLifecycleOwner, object: Observer<Status>{
            override fun onChanged(t: Status?) {
                adaptCrollerView(t as AvReceiverStatus)
                Log.e("AvReceiverFragment", "Der Status hat sich geändert. isActive: ${t.isActive}, mode: ${t.mode}, vol: ${t.vol}")
            }
        })

        return view
    }

    private fun getCrollerOnChangeListener(
        avReceiver: DeviceManager.DeviceViewModel?,
        action: Action,
        view: View
    ): OnCrollerChangeListener {

        return object: OnCrollerChangeListener {
            override fun onProgressChanged(croller: Croller?, progress: Int) {
                if (avReceiver?.getStatus()?.value?.isActive!!) {
                    view.volumeText.text = (progress + 40).toString()
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
                val seekbarProgress = croller?.progress!!
                val volString = (seekbarProgress + 40).toString().padStart(3, '0') + "VL"
                action.commandName = "{\"turn\":\"$volString\"}"

                GlobalScope.launch { executeCommand(action, avReceiver) }
            }
        }
    }

    private suspend fun executeCommand(action: Action, avReceiver: DeviceManager.DeviceViewModel?) {
        GlobalScope.async(IO) {
            globalDeviceManager.executeCommand(action)
        }.await()

        withContext(Main) {
            //adaptCrollerView(avReceiver)
        }
    }

    private fun adaptCrollerView(avReceiver: AvReceiverStatus) {
        //val avRecStatus = (avReceiver?.dInstance as ArduinoAvReceiver).deviceStatus

        val isOn = avReceiver.isActive as Boolean
        val vol = avReceiver.vol as Int

        //(avRecViewModel as ArduinoAvReceiver).currentVolume.value = avRecStatus?.vol?.toInt()

        if (isOn) {
            val avRecVolume = vol
            volumeText.text = vol.toString().trimStart('0')
            croller.progress = vol - 40
            croller.indicatorColor = resources.getColor(R.color.colorAccent, context?.theme)
        } else {
            volumeText.text = resources.getString(R.string.text_croller)
            croller.progress = 0
            croller.indicatorColor = View.GONE
        }
    }
}