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
import android.util.Log

class TrackingService : Service(), LocationListener {
    private val TAG = "TrackingService"
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
        Log.d(TAG, "onCreate called")
        trackingBinder = TrackingBinder()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d(TAG, "GPS provider enabled: $gpsEnabled")
        if(!gpsEnabled) {
            Log.d(TAG, "GPS not enabled, returning early")
            return
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 2f, this)
            Log.d(TAG, "Location updates requested successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind called")
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
        Log.d(TAG, "onLocationChanged: lat=${location.latitude}, lng=${location.longitude}")
        if(msgHandler != null){
            val bundle = Bundle()
            bundle.putDouble(LAT_KEY, location.latitude)
            bundle.putDouble(LONG_KEY, location.longitude)
            val message = msgHandler!!.obtainMessage()
            message.data = bundle
            message.what = MSG_LATLNG_VALUE
            msgHandler!!.sendMessage(message)
            Log.d(TAG, "Location message sent to handler")
        } else {
            Log.d(TAG, "msgHandler is null, cannot send location")
        }
    }

    inner class TrackingBinder : Binder() {
        fun setmsgHandler(msgHandler: Handler) {
            this@TrackingService.msgHandler = msgHandler
        }
    }
}