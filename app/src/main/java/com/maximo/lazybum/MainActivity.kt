package com.maximo.lazybum

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.maximo.lazybum.Globals.AVREC_TAB_POS
import com.maximo.lazybum.Globals.DEVICES_TAB_POS
import com.maximo.lazybum.Globals.SCENES_TAB_POS
import com.maximo.lazybum.Globals.SHUTTER_TAB_POS
import com.maximo.lazybum.Globals.VACUUM_TAB_POS
import com.maximo.lazybum.Globals.avReceiverGroups
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.Globals.devicesGroups
import com.maximo.lazybum.Globals.scenesGroups
import com.maximo.lazybum.Globals.shuttersGroups
import com.maximo.lazybum.Globals.vacuumGroups
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.layoutComponents.Action
import com.maximo.lazybum.layoutComponents.Group
import com.maximo.lazybum.layoutComponents.LayoutGroup
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val recordRequestCode = 101
    lateinit var statusHandler: Handler

    private val updateStatusesTask = object : Runnable {
        override fun run() {
            getInitialStatuses()
            statusHandler.postDelayed(this, 5000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        deviceManager = DeviceManager(this)
        statusHandler = Handler(Looper.getMainLooper())

        setupPermissions()
        val continueOk = readLayoutConfigFile()

        if (continueOk) {
            setContentView(R.layout.activity_main)

            val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
            val viewPager: ViewPager = findViewById(R.id.view_pager)
            viewPager.adapter = sectionsPagerAdapter
            val tabs: TabLayout = findViewById(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
        } else {
            Toast.makeText(this, getString(R.string.read_file_error), Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        statusHandler.post(updateStatusesTask)
    }

    override fun onPause() {
        super.onPause()
        statusHandler.removeCallbacks(updateStatusesTask)
    }

    private fun getInitialStatuses() {
        val connMgr = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSSIDs.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
            for (device in deviceManager.myDevices) {
                deviceManager.launchAction(this, Action(device.dName, getString(R.string.status_request_command)))
            }
        } else {
            Toast.makeText(this, R.string.not_at_home, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readLayoutConfigFile(): Boolean {

        val layoutConfig = resources.openRawResource(R.raw.layout_config)
            .bufferedReader().use { it.readText() }
        val type = object: TypeToken<List<List<LayoutGroup>>>() {}.type

        try {
            val layoutTabList: List<List<LayoutGroup>> = Gson().fromJson(layoutConfig, type)

            devicesGroups = toGroupList(layoutTabList[DEVICES_TAB_POS])
            scenesGroups = toGroupList(layoutTabList[SCENES_TAB_POS])
            avReceiverGroups = toGroupList(layoutTabList[AVREC_TAB_POS])
            shuttersGroups = toGroupList(layoutTabList[SHUTTER_TAB_POS])
            vacuumGroups = toGroupList(layoutTabList[VACUUM_TAB_POS])

        } catch (ioException: IOException) {
            Toast.makeText(this, getString(R.string.read_layout_config_failed), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    fun toGroupList(layoutGroupList: List<LayoutGroup>?): List<Group> {
        val groupList: MutableList<Group> = mutableListOf()
        if (layoutGroupList != null) {
            for (layoutGroup in layoutGroupList) {
                groupList.add(layoutGroup.toGroup())
            }
        }
        return groupList
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), recordRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            recordRequestCode -> {
                try {
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, getString(R.string.permission_request_denied), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.permission_request_granted), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.permission_request_problem), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}