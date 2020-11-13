package com.maximo.lazybum

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.maximo.lazybum.Globals.avReceiverFragmentGroups
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.Globals.devicesFragmentGroups
import com.maximo.lazybum.Globals.scenesFragmentGroups
import com.maximo.lazybum.Globals.shutterFragmentGroups
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.layoutComponents.Action
import com.maximo.lazybum.layoutComponents.Tab
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private val recordRequestCode = 101

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        deviceManager = DeviceManager(this)
        getInitialStatuses()

        setupPermissions()
        readLayoutConfigFile()

        setContentView(R.layout.activity_main)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }

    override fun onResume() {
        super.onResume()
        getInitialStatuses()
    }

    private fun getInitialStatuses() {
        for (device in deviceManager.myDevices) {
            deviceManager.launchAction(this, Action(getString(R.string.status_request_command), device.dName))
        }
    }

    private fun readLayoutConfigFile(): Boolean {
        val layoutConfigFile = resources.openRawResource(R.raw.layout_config)
        val type = object: TypeToken<List<Tab>>() {}.type
        try {
            val tabsList: List<Tab> = Gson().fromJson(InputStreamReader(layoutConfigFile), type)

            devicesFragmentGroups = tabsList[DEVICES_TAB_POS].groups
            scenesFragmentGroups = tabsList[SCENES_TAB_POS].groups
            avReceiverFragmentGroups = tabsList[AVREC_TAB_POS].groups
            shutterFragmentGroups = tabsList[SHUTTER_TAB_POS].groups
        } catch (ioException: IOException) {
            Toast.makeText(this, getString(R.string.read_layout_config_failed), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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