package com.maximo.lazybum

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
import com.maximo.lazybum.shellyApi.ShellyShutter
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

class ScenesFragment : Fragment() {

    companion object{

        val sectionHeaderList = mutableListOf<ListRow>(
            ListSectionHeader(id = 0, text = "Bewegtbild"),
            ListSectionHeader(id = 3, text = "Auf die Ohren"),
            ListSectionHeader(id = 6, text = "Licht und Schatten"))
        val sceneList = mutableListOf(
            ListScene(id = 1, text = "Fernsehen", img = R.drawable.ic_football, actionList = mutableListOf(), description = ""),
            ListScene(id = 2, text = "Großleinwand", img = R.drawable.ic_football, actionList = mutableListOf(), description = ""),
            ListScene(id = 4, text = "Spotify", img = R.drawable.ic_football, actionList = mutableListOf(), description = ""),
            ListScene(id = 5, text = "Bose Soundtouch", img = R.drawable.ic_football, actionList = mutableListOf(), description = ""),
            ListScene(id = 7, text = "Soirée", img = R.drawable.ic_football, actionList = mutableListOf(), description = ""),
            ListScene(id = 8, text = "Heißer Tag", img = R.drawable.ic_football, actionList = mutableListOf(), description = ""))
        var nextGo = "close"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val btnListView: MutableList<ListRow> = sceneList.union(sectionHeaderList).sortedBy { it.id }.toMutableList()

        val view = inflater.inflate(R.layout.list, container, false)
        val listView = view.list
        listView.adapter = MyListAdapter(requireContext(), btnListView)
        listView.setOnItemClickListener { parent, v, position, id ->
            val connMgr = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!btnListView[position].isHeader) {
                if (Globals.supportedWifiSsids.contains(connMgr.connectionInfo.ssid.filterNot { it == '\"' })) {
                    val clickedDevice = btnListView[position] as ListAction
                    execute(clickedDevice, clickedDevice.cmd, listView)
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
            for (scene in sceneList) {
                //execute(scene as ListAction, CmdInterface("", "getStatus", "", "", "", 0), listView)
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
            .baseUrl(listAction.url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO) {

            val response: Response<JsonObject>

            try {
                when (listAction.id.toInt()) {
                    6 -> {
                        if (cmd.action == "getStatus") {
                            response = api.getShutterStatus().awaitResponse()
                        } else {
                            response = api.go(nextGo).awaitResponse()
                        }
                        if (response.isSuccessful) {
                            val data = response.body()
                            val shutter = Gson().fromJson(data, ShellyShutter::class.java)

                            if (shutter.state == "stop") {
                                if (shutter.last_direction == "close") {
                                    nextGo = "open"
                                } else {
                                    nextGo = "close"
                                }
                            } else {
                                nextGo = "stop"
                            }
                        }
                    }
                    else -> { }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}