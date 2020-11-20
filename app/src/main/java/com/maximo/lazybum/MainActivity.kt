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
import com.maximo.lazybum.Globals.avReceiverFragmentGroups
import com.maximo.lazybum.Globals.deviceManager
import com.maximo.lazybum.Globals.devicesFragmentGroups
import com.maximo.lazybum.Globals.scenesFragmentGroups
import com.maximo.lazybum.Globals.shutterFragmentGroups
import com.maximo.lazybum.Globals.vacuumFragmentGroups
import com.maximo.lazybum.deviceComponents.DeviceManager
import com.maximo.lazybum.layoutComponents.Action
import com.maximo.lazybum.layoutComponents.Group
import com.maximo.lazybum.layoutComponents.Item

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

        devicesFragmentGroups = devicesTabGroups
        scenesFragmentGroups = scenesTabGroups
        avReceiverFragmentGroups = avRecTabGroups
        shutterFragmentGroups = shutterTabGroups
        vacuumFragmentGroups = vacuumTabGroups

        /*
        val devicesLayoutConfig = resources.openRawResource(R.raw.layout_config_devices)
            .bufferedReader().use { it.readText() }
        val groupType = object: TypeToken<List<Group>>() {}.type
        val devicesGroups: List<Group> = Gson().fromJson(devicesLayoutConfig, groupType)

        val layoutConfig = resources.openRawResource(R.raw.layout_config)
            .bufferedReader().use { it.readText() }
        val type = object: TypeToken<List<Tab>>() {}.type
        try {
            val tabsList: List<Tab> = Gson().fromJson(layoutConfig, type)

            devicesFragmentGroups = tabsList[DEVICES_TAB_POS].groupList
            scenesFragmentGroups = tabsList[SCENES_TAB_POS].groupList
            avReceiverFragmentGroups = tabsList[AVREC_TAB_POS].groupList
            shutterFragmentGroups = tabsList[SHUTTER_TAB_POS].groupList

        } catch (ioException: IOException) {
            Toast.makeText(this, getString(R.string.read_layout_config_failed), Toast.LENGTH_SHORT).show()
            return false
        }

         */
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

    companion object {
        val devicesTabGroups: List<Group> = listOf(
            Group("Küche", listOf(
                Item("Kaffeemaschine", "wechselnd an | aus", "ic_coffee", listOf(
                    Action("coffeeSwitch", "toggle"))))
            ),
            Group("Essbereich", listOf(
                Item("Esstischlampe", "wechselnd an | aus", "ic_dining", listOf(
                    Action("diningLight", "toggle"))))
            ),
            Group("Wohnzimmer", listOf(
                Item("LED Grid",
                    "wechselnd an | aus - lang drücken für mehr",
                    "ic_led_grid",
                    listOf(
                        Action("ledGridSwitch", "toggle"))),
                Item("Strahler", "wechselnd an | aus - lang drücken für mehr", "ic_spots", listOf(
                    Action("spotLight", "{\"turn\":\"toggle\",\"brightness\":\"40\"}"))),
                Item("Fernseher", "wechselnd an | aus", "ic_monitor", listOf(
                    Action("tvSwitch", "toggle"))),
                Item("TV Receiver", "wechselnd an | aus", "ic_sky", listOf(
                    Action("arduinoSkyReceiver", "{\"turn\":\"toggleSky\"}"))))
            )
        )
        val scenesTabGroups: List<Group> = listOf(
            Group("Bewegtbild", listOf(
                Item("Fernsehen", "LED Grid gedimmt - Spots aus - TV an", "ic_monitor", listOf(
                    Action("ledGridSwitch",
                        "{\"color\":\"33000000\",\"mode\":\"rgb\",\"action\":\"on\",\"ramp\":\"2000\"}"),
                    Action("spotLight", "{\"turn\":\"off\",\"brightness\":\"40\"}"),
                    Action("tvSwitch", "{\"turn\":\"on\"}"),
                    Action("arduinoSkyReceiver", "{\"turn\":\"on\"}"),
                    Action("arduinoAvReceiver", "{\"turn\":\"TV\"}"))),
                Item("Großleinwand",
                    "LED Grid aus - Spots aus - Receiver an",
                    "ic_football",
                    listOf(
                        Action("ledGridSwitch",
                            "{\"color\":\"33000000\",\"mode\":\"rgb\",\"action\":\"off\",\"ramp\":\"2000\"}"),
                        Action("spotLight", "{\"turn\":\"off\",\"brightness\":\"40\"}"),
                        Action("tvSwitch", "{\"turn\":\"off\"}"),
                        Action("arduinoSkyReceiver", "{\"turn\":\"on\"}"),
                        Action("arduinoAvReceiver", "{\"turn\":\"TV\"}"))))),
            Group("Auf die Ohren", listOf(
                Item("Spotify", "lang drücken für Wechsel zur App", "ic_music", listOf(
                    Action("arduinoAvReceiver", "{\"turn\":\"Bose\"}"))))
            ),
            Group("Licht und Schatten", listOf(
                Item("Soirée", "gedimmtes Licht im Wohnzimmer", "ic_dining", listOf(
                    Action("ledGridSwitch",
                        "{\"color\":\"99000000\",\"mode\":\"rgb\",\"action\":\"on\",\"ramp\":\"2000\"}"),
                    Action("spotLight", "{\"turn\":\"on\",\"brightness\":\"20\"}"))),
                Item("Heißer Tag", "alle Rollos runter", "ic_shutter", listOf(
                    Action("shutterParents", "close"),
                    Action("shutterBathroom", "close"),
                    Action("shutterWork", "close"),
                    Action("shutterChildLeft", "close"),
                    Action("shutterChildRight", "close"),
                    Action("shutterLivingPergola", "close"),
                    Action("shutterLivingSofa", "close"),
                    Action("shutterLivingDoor", "close"),
                    Action("shutterLivingSpices", "close"))))),
            Group("Sonstiges", listOf(
                Item("Alles ausschalten", "schaltet alles aus", "ic_power_off", listOf(
                    Action("ledGridSwitch",
                        "{\"color\":\"33000000\",\"mode\":\"rgb\",\"action\":\"off\",\"ramp\":\"2000\"}"),
                    Action("spotLight", "{\"turn\":\"off\",\"brightness\":\"40\"}"),
                    Action("tvSwitch", "{\"turn\":\"off\"}"),
                    Action("arduinoSkyReceiver", "{\"turn\":\"off\"}"),
                    Action("arduinoAvReceiver", "{\"turn\":\"DvcsOff\"}"))))
            )
        )
        val avRecTabGroups : List<Group> = listOf(
            Group("Modus einstellen", listOf(
                Item("Fernsehen", "Quelle Sky Receiver", "ic_football", listOf(
                    Action("arduinoAvReceiver", "{\"turn\":\"TV\"}"))),
                Item("Musik über Bose System", "Quelle Bose Adapter", "ic_music", listOf(
                    Action("arduinoAvReceiver", "{\"turn\":\"Bose\"}"))),
                Item("Musik oder Video über Chromecast", "Quelle Chromecast", "ic_film", listOf(
                    Action("arduinoAvReceiver", "{\"turn\":\"CCaudio\"}"))))
            ),
            Group("Sonstige", listOf(
                Item("Ausschalten", "schaltet AV Receiver aus", "ic_power_off", listOf(
                    Action("arduinoAvReceiver", "{\"turn\":\"DvcsOff\"}"))))
            )
        )
        val shutterTabGroups: List<Group> = listOf(
            Group("Elternzimmer", listOf(
                Item("Schlafzimmer", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterParents", "next"))),
                Item("Bad", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterBathroom", "next"))))),
            Group("Arbeitszimmer", listOf(
                Item("es gibt nur eins", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterWork", "next"))))),
            Group("Kinderzimmer", listOf(
                Item("links", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterChildLeft", "next"))),
                Item("rechts", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterChildRight", "next"))))),
            Group("Wohnzimmer", listOf(
                Item("zur Pergola", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterLivingPergola", "next"))),
                Item("hinterm Sofa", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterLivingSofa", "next"))),
                Item("zur Terassentür", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterLivingDoor", "next"))),
                Item("zur Kräuterschnecke", "wechselnd runter | stop | hoch", "ic_shutter", listOf(
                    Action("shutterLivingSpices", "next")))))
        )
        val vacuumTabGroups: List<Group> = listOf(
            Group("Zonenreinigung", listOf(
                Item("Küche", "Roboter saugt Küchenbereich", "ic_kitchen", listOf(
                    Action("roborock", "[[20549,16454,22099,18524,1]]"))),
                Item("Schlafzimmer", "Roboter saugt Schlafzimmer", "ic_sleeping", listOf(
                    Action("roborock", "[[22050,28386,26309,31419,1]]"))))
            ),
            Group("Steuerbefehle", listOf(
                Item("Stop", "Roboter bleibt stehen", "ic_halt", listOf(
                    Action("roborock", "stop"))),
                Item("Zur Ladestation", "Roboter fährt zur Ladestation zurück", "ic_charging_station", listOf(
                    Action("roborock", "home"))))
            )
        )
    }
}