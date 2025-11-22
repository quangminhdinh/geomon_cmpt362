package com.example.myapplication.services.tracking

import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder

class TrackingService : Service(), LocationListener {
    private lateinit var  trackingBinder: TrackingBinder
    private lateinit var locationManager: LocationManager
    private var msgHandler: Handler? = null

    companion object{
        const val LAT_KEY = "lat key"
        const val LONG_KEY = "long key"
        const val MSG_LATLNG_VALUE = 0
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return

        trackingBinder = TrackingBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 2f, this)
        } catch (e: SecurityException) { }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return trackingBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        msgHandler = null
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        reset()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        reset()
        stopSelf()
    }

    private fun reset(){
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        if(msgHandler != null){
            val bundle = Bundle()
            bundle.putDouble(LAT_KEY, location.latitude)
            bundle.putDouble(LONG_KEY, location.longitude)
            val message = msgHandler!!.obtainMessage()
            message.data = bundle
            message.what = MSG_LATLNG_VALUE
            msgHandler!!.sendMessage(message)
        }
    }

    inner class TrackingBinder : Binder() {
        fun setmsgHandler(msgHandler: Handler) {
            this@TrackingService.msgHandler = msgHandler
        }
    }
}