package com.example.myapplication
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: Activity) {

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request location permissions
     */
    fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Check and request permissions if needed
     * Returns true if permissions already granted
     */
    fun checkAndRequestPermissions(): Boolean {
        return if (hasLocationPermissions()) {
            true
        } else {
            requestLocationPermissions()
            false
        }
    }

    /**
     * Handle the permission request result
     * Call this from your Activity's onRequestPermissionsResult
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                // All permissions granted
                onGranted()
            } else {
                // Permission denied
                Toast.makeText(
                    activity,
                    "Location permission is required for this app",
                    Toast.LENGTH_LONG
                ).show()
                onDenied()
            }
        }
    }
}