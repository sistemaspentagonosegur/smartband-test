package com.csalazar.appreloj

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.csalazar.appreloj.domain.Smartband
import com.oudmon.ble.base.bluetooth.BleAction
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.bluetooth.QCBluetoothCallbackCloneReceiver
import com.oudmon.ble.base.communication.Constants
import com.oudmon.ble.base.communication.LargeDataHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class MyBluetoothReceiver(private val context: Context) {
    fun receive(): Flow<BluetoothConnectionEvent> = callbackFlow {
        val receiver = object : QCBluetoothCallbackCloneReceiver() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun connectStatue(device: BluetoothDevice?, connected: Boolean) {
                if (device != null && connected) {
                    if (device.name != null) {
                        DeviceManager.getInstance().deviceName = device.name
                        SmartbandRepository.getInstance(context).saveSmartband(
                            Smartband(
                                device.name, device.address, 0
                            )
                        )
                    }
                } else {
                    Log.d("MqttService", "Disconected ${device?.name}")

                    trySend(BluetoothConnectionEvent(false))
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServiceDiscovered() {
                LargeDataHandler.getInstance().initEnable()
                trySend(BluetoothConnectionEvent(true))
            }

            override fun onCharacteristicChange(address: String?, uuid: String?, data: ByteArray?) {
            }

            override fun onCharacteristicRead(uuid: String?, data: ByteArray?) {
                if (uuid != null && data != null) {
                    val version = String(data, Charsets.UTF_8)
                    when (uuid) {
                        Constants.CHAR_FIRMWARE_REVISION.toString() -> {
                            Log.e("rom----", version)
                            //rom  version
                            MyApplication.getInstance.firmwareVersion = version
                        }

                        Constants.CHAR_HW_REVISION.toString() -> {
                            //hardware  version
                            Log.e("hardware----", version)
                            MyApplication.getInstance.hardwareVersion = version
                        }
                    }
                }
            }

        }
        val intentFilter = BleAction.getIntentFilter()
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Manejo por si el receiver ya había sido desregistrado
            }
        }
    }.flowOn(Dispatchers.Main)

}