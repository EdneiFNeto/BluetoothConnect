package com.arca.bluetoothconnection.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.arca.bluetoothconnection.MainActivity

interface BluetoothInterface {
    fun initialize()
    fun devicesPaired(): List<Device>?
    fun connectDevice(device: Device): Boolean
    fun listDevices(): List<Device>
    fun activeBluetooth()
    fun isBluetoothEnabled(): Boolean
    fun onDiscoveryStarted()
}

class BluetoothImplement constructor(
    private val context: Context,
    private val activity: Activity
) : BluetoothInterface {

    private val devices: ArrayList<Device> = arrayListOf()
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    override fun initialize() {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun devicesPaired(): List<Device> {
        clearDevices()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activity.requestPermission()
                return emptyList()
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.filter { device ->
            device.name != null && device.address != null
        }?.onEach {
            devices.add(Device(name = it.name, address = it.address, connected = true))
        }

        return devices
    }

    override fun connectDevice(device: Device) : Boolean {
        val remoteDevice: BluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activity.requestPermission()
                return false
            }
        }
        return remoteDevice.createBond()
    }

    override fun listDevices(): List<Device> = devices
        .filter { it.name != null }.distinct()

    override fun activeBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Check permission BLUETOOTH_CONNECT")
                activity.requestPermission()
                return
            }
        }

        Log.d(TAG, "bluetoothAdapter isEnabled ${bluetoothAdapter.isEnabled}")

        if (!bluetoothAdapter.isEnabled) {
            activity.startActivityForResult(enableBtIntent, 101)
        }
    }

    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter.isEnabled

    override fun onDiscoveryStarted() {
        clearDevices()
        val permissionUtil = Permissions(activity, context)

        Log.d(TAG, "discovery called SDK_INT ${Build.VERSION.SDK_INT}")
        if (!permissionUtil.getPermissionAndroidOreo()) {
            permissionUtil.requestPermissionOreo()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "requestPermission called")
                activity.requestPermission()
                return
            }
        }

        Log.d(TAG, "isDiscovering ${bluetoothAdapter.isDiscovering}")

        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        activity.registerReceiver(Receiver(), getIntentFilter())
        bluetoothAdapter.startDiscovery()
    }

    private fun getIntentFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        return filter
    }

    private fun clearDevices() {
        devices.clear()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun Activity.requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            103
        )
    }

    inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d(TAG, "Check permission BLUETOOTH_CONNECT")
                            activity.requestPermission()
                            return
                        }
                    }

                    if (!device?.name.isNullOrEmpty())
                        devices.add(Device(name = device?.name, address = device?.address))

                    Log.w(TAG, "ACTION_FOUND $devices")
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    Log.w(TAG, "ACTION_BOND_STATE_CHANGED")
                    bluetoothAdapter.bondedDevices
                        .filter { !it.name.isNullOrEmpty() }
                        .onEach {
                            Log.w(TAG, "bondedDevices ${it.name}")
                            devices.add(Device(it.name, it.address, true))
                        }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.w(TAG, "ACTION_DISCOVERY_FINISHED")
                }

                else -> {
                    Log.w(TAG, "Not action received")
                }
            }
        }
    }

    companion object {
        const val TAG = "BluetoothImple"
    }
}


class Bluetooth(private val service: BluetoothInterface) {

    fun initialize() {
        service.initialize()
    }

    fun onDiscoveryStarted() {
        service.onDiscoveryStarted()
    }

    fun activeBluetooth() {
        service.activeBluetooth()
    }

    fun isActiveBluetooth(): Boolean = service.isBluetoothEnabled()

    fun devicesPaired(): List<Device>? = service.devicesPaired()

    fun listDevices(): List<Device> = service.listDevices()

    fun connectDevice(device: Device): Boolean = service.connectDevice(device)
}

data class Device(val name: String?, val address: String?, val connected: Boolean = false) {
    override fun toString(): String {
        return String.format("Name: %s\nAddress: %s, connected: %s", name, address, connected)
    }
}
