package com.arca.bluetoothconnection.bluetooth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class Permissions(
    private val activity: Activity,
    private val context: Context
) {

    init {
        requestPermission()
    }

    fun requestPermission() {
        val permissionsToRequest = mutableListOf<String>()
        if (!getPermission()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), PERMISSIONS)
        }
    }

    fun getPermission() = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun getPermissionAndroidOreo(): Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        }

        return false
    }

    fun requestPermissionOreo() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()
            if (!getPermissionAndroidOreo()) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), PERMISSIONS)
            }
        }
    }

    private companion object {
         const val PERMISSIONS = 101
    }
}