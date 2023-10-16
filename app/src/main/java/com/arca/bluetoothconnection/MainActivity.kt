package com.arca.bluetoothconnection

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.arca.bluetoothconnection.bluetooth.Bluetooth
import com.arca.bluetoothconnection.bluetooth.BluetoothImplement
import com.arca.bluetoothconnection.bluetooth.Device
import com.arca.bluetoothconnection.bluetooth.Permissions
import com.arca.bluetoothconnection.databinding.ActivityMainBinding
import com.arca.bluetoothconnection.databinding.ListDevicesBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var permissionUtil: Permissions
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetooth: Bluetooth
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionUtil = Permissions(this, this)
        bluetooth = Bluetooth(BluetoothImplement(this, this))
    }

    override fun onResume() {
        super.onResume()

        if (!permissionUtil.getPermission()) {
            permissionUtil.requestPermission()
            return
        }

        bluetooth.apply {
            initialize()
            activeBluetooth()

            val list = devicesPaired() ?: arrayListOf()
            onSetupAdapter(list)

            binding.switchMaterial.apply {
                isEnabled = isActiveBluetooth()
                isChecked = isEnabled
            }

            binding.button.setOnClickListener {
                startTimeCountDown()
                onDiscoveryStarted()
                showProgressBar()
            }
        }
    }

    private fun onSetupAdapter(list: List<Device>) {
        deviceAdapter = DeviceAdapter(list)
        binding.recycleView.apply { adapter = deviceAdapter }
        deviceAdapter.handleClick = {
            Log.d(TAG, "handleClick $it")
            bluetooth.connectDevice(it)
        }
    }

    private fun startTimeCountDown() {
        countDownTimer.apply {
            cancel()
            start()
        }
    }

    private fun showProgressBar() {
        binding.progressBar.isVisible = true
    }

    private val countDownTimer = object : CountDownTimer(30000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val sec = convertToSec(millisUntilFinished)
            val min = convertToHour(millisUntilFinished)
            val timer = String.format(
                "%s:%s",
                "${if (min < 10) "0$min" else min}",
                "${if (sec < 10) "0$sec" else sec}"
            )

            if (bluetooth.listDevices().isEmpty()) {
                Log.w(TAG, "Timer: $timer")
            } else {
                onSetupAdapter(bluetooth.listDevices())
                hideProgressBar()
                cancel()
            }
        }

        override fun onFinish() {
            Log.d(TAG, "On finish")
            hideProgressBar()
        }
    }

    private fun hideProgressBar() {
        binding.progressBar.isVisible = false
    }

    private fun convertToSec(millisUntilFinished: Long) =
        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                millisUntilFinished
            )
        )

    private fun convertToHour(millisUntilFinished: Long) =
        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished))

    companion object {
        const val TAG = "MainActivityLog"
    }
}

class DeviceAdapter(
    private val list: List<Device> = arrayListOf(),
    var handleClick: (Device) -> Unit = {}
) : RecyclerView.Adapter<DeviceAdapter.Holder>() {

    inner class Holder(private val binding: ListDevicesBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.eventListener = this
        }

        private lateinit var device: Device

        fun add(myDevice: Device) {
            binding.devices = myDevice
            device = myDevice

            if (device.connected) {
                binding.apply {
                    cardView.setCardBackgroundColor(Color.parseColor("#673AB7"))
                    name.setTextColor(Color.parseColor("#FFFFFF"))
                    address.setTextColor(Color.parseColor("#FFFFFF"))
                }
            } else{
                binding.apply {
                    cardView.setCardBackgroundColor(Color.parseColor("#F1EFF3"))
                    name.setTextColor(Color.parseColor("#FF000000"))
                    address.setTextColor(Color.parseColor("#FF000000"))
                }
            }
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                binding.cardView.id -> {
                    handleClick(device)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            ListDevicesBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.add(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}