package com.maximo.lazybum.fragments

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.ApiRequest
import com.maximo.lazybum.Globals
import com.maximo.lazybum.R
import com.maximo.lazybum.commands.Cmd
import com.maximo.lazybum.commands.CmdInterface
import com.maximo.lazybum.shellyApi.ShellyShutter
import com.maximo.lazybum.uiComponents.*
import kotlinx.android.synthetic.main.list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


private const val baseUrl = "http://192.168.178."

class RollerFragment : Fragment() {

    companion object{

        val sectionHeaderList = mutableListOf(
            ListSectionHeader(id = 0, text = "Elternzimmer"),
            ListSectionHeader(id = 3, text = "Arbeitszimmer"),
            ListSectionHeader(id = 5, text = "Kinderzimmer"))
        val deviceList = mutableListOf(
            ListAction(id = 1, text = "Schlafzimmer", img = R.drawable.ic_shutter, description = "wechselnd runter | stop | hoch",
                action = Action(deviceId = 55, url = baseUrl + "55", cmd = Cmd(""), nextCmd = "close")),
            ListAction(id = 2, text = "Bad", img = R.drawable.ic_shutter, description = "wechselnd runter | stop | hoch",
                action = Action(deviceId = 54, url = baseUrl + "54", cmd = Cmd(""), nextCmd = "close")),
            ListAction(id = 4, text = "es gibt nur eins", img = R.drawable.ic_shutter, description = "wechselnd runter | stop | hoch",
                action = Action(deviceId = 57, url = baseUrl + "57", cmd = Cmd(""), nextCmd = "close")),
            ListAction(id = 6, text = "links", img = R.drawable.ic_shutter, description = "wechselnd runter | stop | hoch",
                action = Action(deviceId = 51, url = baseUrl + "51", cmd = Cmd(""), nextCmd = "close")),
            ListAction(id = 7, text = "rechts", img = R.drawable.ic_shutter, description = "wechselnd runter | stop | hoch",
                action = Action(deviceId = 56, url = baseUrl + "56", cmd = Cmd(""), nextCmd = "close")))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val btnListView: MutableList<ListRow> = deviceList.union(sectionHeaderList).sortedBy { it.id }.toMutableList()

        val view = inflater.inflate(R.layout.list, container, false)
        val listView = view.list
        listView.adapter = MyListAdapter(requireContext(), btnListView)
        listView.setOnItemClickListener { parent, v, position, id ->
            val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!btnListView[position].isHeader) {
                if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                    val clickedDevice = btnListView[position] as ListAction
                    execute(clickedDevice, clickedDevice.action.cmd, listView)
                } else {
                    Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
            val listView = requireView().list
            for (device in deviceList) {
                execute(device, Cmd("getStatus"), listView)
            }
        } else {
            Toast.makeText(context, "Not at home", Toast.LENGTH_SHORT).show()
        }
    }

    private fun execute(listAction: ListAction, cmd: CmdInterface, listView: ListView) {
        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(3, TimeUnit.SECONDS)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(listAction.action.url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO) {

            val response: Response<JsonObject>

            try {
                if (cmd.action == "getStatus") {
                    response = api.getShutterStatus().awaitResponse()
                } else {
                    response = api.go(listAction.action.nextCmd).awaitResponse()
                }
                if (response.isSuccessful) {
                    val data = response.body()
                    val shutter = Gson().fromJson(data, ShellyShutter::class.java)

                    if (shutter.state == "stop") {
                        if (shutter.last_direction == "close") {
                            listAction.action.nextCmd = "open"
                        } else {
                            listAction.action.nextCmd = "close"
                        }
                    } else {
                        listAction.action.nextCmd = "stop"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}