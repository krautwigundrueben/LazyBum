package com.maximo.lazybum

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
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
import com.maximo.lazybum.Globals.devicesFragmentGroups
import com.maximo.lazybum.Globals.globalDeviceManager
import com.maximo.lazybum.Globals.scenesFragmentGroups
import com.maximo.lazybum.Globals.shutterFragmentGroups
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.deviceComponents.dataClasses.DeviceClass
import com.maximo.lazybum.layoutComponents.Tab
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences
    val TAG = "MainActivity"
    private val RECORD_REQUEST_CODE = 101

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        // TODO: StreamReader optimieren? siehe: https://bezkoder.com/kotlin-android-read-json-file-assets-gson/ oder https://www.spigotmc.org/threads/json-configuration-files.212794/

        val deviceConfigFile = resources.openRawResource(R.raw.devices_config)
        val listDeviceType = object : TypeToken<List<DeviceClass>>() {}.type
        val initialDeviceList: List<DeviceClass> =
            Gson().fromJson(InputStreamReader(deviceConfigFile), listDeviceType)
        globalDeviceManager = DeviceManager(this, initialDeviceList)

        val layoutConfigFile = resources.openRawResource(R.raw.layout_config)
        val type = object: TypeToken<List<Tab>>() {}.type
        val tabsList: List<Tab> = Gson().fromJson(InputStreamReader(layoutConfigFile), type)

        devicesFragmentGroups = tabsList[DEVICES_TAB_POS].groups
        scenesFragmentGroups = tabsList[SCENES_TAB_POS].groups
        avReceiverFragmentGroups = tabsList[AVREC_TAB_POS].groups
        shutterFragmentGroups = tabsList[SHUTTER_TAB_POS].groups

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val isLargeAppearance = when (displayMetrics.scaledDensity.toInt()) {
            4 -> true
            else -> false
        }

        // TODO: braucht's das?
        sharedPreferences = getSharedPreferences("app", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLargeAppearance", isLargeAppearance)
        editor.apply()

        setupPermissions()
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), RECORD_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }
        }
    }
}