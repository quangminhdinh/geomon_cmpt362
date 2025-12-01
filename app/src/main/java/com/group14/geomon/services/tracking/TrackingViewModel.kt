package com.group14.geomon.services.tracking

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.group14.geomon.services.tracking.TrackingService.Companion.LAT_KEY
import com.group14.geomon.services.tracking.TrackingService.Companion.LONG_KEY
import com.google.android.gms.maps.model.LatLng

class TrackingViewModel : ViewModel(), ServiceConnection {
    private var trackingMessageHandler: TrackingMessageHandler = TrackingMessageHandler(
        Looper.getMainLooper())

    var serviceStarted = MutableLiveData<Boolean>().apply { value = false }

    private val _latLng = MutableLiveData<LatLng>()
    val latLng: LiveData<LatLng>
        get() {
            return _latLng
        }

    override fun onServiceConnected(
        name: ComponentName?,
        service: IBinder?
    ) {
        val binder = service as TrackingService.TrackingBinder
        binder.setmsgHandler(trackingMessageHandler)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    inner class TrackingMessageHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == TrackingService.MSG_LATLNG_VALUE) {
                val bundle = msg.data
                val latitude = bundle.getDouble(LAT_KEY)
                val longitude = bundle.getDouble(LONG_KEY)
                val curLatLng = LatLng(latitude, longitude)
                _latLng.value = curLatLng
            }
        }
    }
}
