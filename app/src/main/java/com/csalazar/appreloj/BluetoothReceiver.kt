package com.csalazar.appreloj

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.csalazar.appreloj.util.startMqttService
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val connectState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (connectState == BluetoothAdapter.STATE_OFF) {

                    Log.i("qc" ,"蓝牙关闭了 --> ")
                    BleOperateManager.getInstance().setBluetoothTurnOff(false)
                    BleOperateManager.getInstance().disconnect()
                    EventRepository.getInstance(context.applicationContext).emitEvent(
                        BluetoothConnectionEvent(false))
                } else if (connectState == BluetoothAdapter.STATE_ON) {
                    Log.i("qc" ,"蓝牙开启了 --> ")
                    BleOperateManager.getInstance().setBluetoothTurnOff(true)
                    BleOperateManager.getInstance().reConnectMac=DeviceManager.getInstance().deviceAddress
                    BleOperateManager.getInstance().connectWithScan(DeviceManager.getInstance().deviceAddress)

                }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {

            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {

            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {

            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                context.startMqttService()
            }

        }
    }

}