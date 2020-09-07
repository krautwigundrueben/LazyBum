package com.maximo.lazybum

import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.maximo.lazybum.api.ShellyLight
import com.maximo.lazybum.api.ShellyLightsStatus
import com.maximo.lazybum.api.ShellyRelay
import com.maximo.lazybum.api.ShellyRelayStatus
import com.maximo.lazybum.myStromApi.D8E3D9494
import com.maximo.lazybum.myStromApi.Relay
import kotlinx.android.synthetic.main.row.view.*
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

class MyListAdapter(var mCtx: Context, var resources: Int, var items: MutableList<Device>):ArrayAdapter<Device>(mCtx, resources, items) {

    private var TAG = "ListAdapter"

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater:LayoutInflater = LayoutInflater.from(mCtx)
        val view:View = layoutInflater.inflate(resources, null)

        val imageView:ImageView = view.findViewById(R.id.imageView)
        val titleTextView:TextView = view.findViewById(R.id.textTitle)
        val locationTextView:TextView = view.findViewById(R.id.textLocation)

        val mItem:Device = items[position]
        imageView.setImageDrawable(mCtx.resources.getDrawable(mItem.img))
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorOff), PorterDuff.Mode.SRC_IN)
        titleTextView.text = mItem.title
        locationTextView.text = mItem.location

        view.setOnClickListener {
            execute(mItem, mItem.command, view, mCtx)
        }

        execute(mItem, Command("getStatus", "", "", "", 0), view, mCtx)

        return view
    }

    private fun execute(device: Device, command: Command, view: View, mCtx: Context) {

        val client = OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val api = Retrofit.Builder()
            .baseUrl(device.base_url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiRequest::class.java)

        GlobalScope.launch(Dispatchers.IO) {

            val response: Response<JsonObject>

            try {
                when (device.id) {
                    0, 4 -> {
                        if (command.action == "getStatus") {
                            response = api.getReport().awaitResponse()
                        }
                        else {
                            response = api.toggle().awaitResponse()
                        }
                        if (response.isSuccessful) {
                            val data = response.body()
                            device.isOn = Gson().fromJson(data, Relay::class.java).relay
                        } else {
                            Log.d(TAG, response.body().toString())
                            Log.d(TAG, response.code().toString())
                        }
                    }
                    1 -> {
                        if (command.action == "getStatus") {
                            response =
                                api.getInfo().awaitResponse()
                        }
                        else {
                            response =
                                api.set(command.color, command.mode, command.action, command.ramp)
                                    .awaitResponse()
                        }
                        if (response.isSuccessful) {
                            val data= response.body()
                            val deviceObject= data?.getAsJsonObject("840D8E3D9494")
                            device.isOn = Gson().fromJson<D8E3D9494>(deviceObject, D8E3D9494::class.java).on
                        } else {
                            Log.d(TAG, response.body().toString())
                            Log.d(TAG, response.code().toString())
                        }
                    }
                    2 -> {
                        if (command.action == "getStatus") {
                            response = api.getStatus().awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, ShellyLightsStatus::class.java).lights[0].ison
                            } else {
                                Log.d(TAG, response.body().toString())
                                Log.d(TAG, response.code().toString())
                            }
                        }
                        else {
                            response = api.set(command.action, command.value).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, ShellyLight::class.java).ison
                            } else {
                                Log.d(TAG, response.body().toString())
                                Log.d(TAG, response.code().toString())
                            }
                        }
                    }
                    3 -> {
                        if (command.action == "getStatus") {
                            response = api.getStatus().awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, ShellyRelayStatus::class.java).relays[0].ison
                            } else {
                                Log.d(TAG, response.body().toString())
                                Log.d(TAG, response.code().toString())
                            }
                        }
                        else {
                            response = api.set(command.action).awaitResponse()
                            if (response.isSuccessful) {
                                val data = response.body()
                                device.isOn = Gson().fromJson(data, ShellyRelay::class.java).ison
                            } else {
                                Log.d(TAG, response.body().toString())
                                Log.d(TAG, response.code().toString())
                            }
                        }
                    }
                    else -> {
                        val response: Response<StringBuffer>

                        try {
                            response = api.sendCommand(command.action).awaitResponse()

                            if (response.isSuccessful) {
                                val data = response.body()
                                println(data)
                            } else {
                                Log.d(TAG, response.body().toString())
                                Log.d(TAG, response.code().toString())
                            }

                            withContext(Dispatchers.Main) {

                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                //Toast.makeText(mCtx, "Something went wrong", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (device.isOn == true) {
                        view.imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorOn), PorterDuff.Mode.SRC_IN)
                    }
                    else {
                        view.imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorOff), PorterDuff.Mode.SRC_IN)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(mCtx, "Something went wrong", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}