package com.csalazar.appreloj

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.csalazar.appreloj.domain.Smartband
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class ScannerRepository private constructor(
    context: Context
) {
    private val context : Application = context.applicationContext as Application


    fun scanDevices() : Flow<List<Smartband>> = callbackFlow {
        val callback = object : ScanWrapperCallback {
            private var scanCount : Int = 0
            private val smartbandList = mutableListOf<Smartband>()

            override fun onStart() {
                smartbandList.clear()
                trySend(smartbandList.toList())
            }

            override fun onStop() {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onLeScan(
                device: BluetoothDevice?,
                rssi: Int,
                p2: ByteArray?
            ) {


                if (device != null && (!device.name.isNullOrEmpty())) {
                    val smartband = Smartband(device.name, device.address, rssi)
                    if (!smartbandList.contains(smartband)) {
                        scanCount++
                        smartbandList.add(0, smartband)
                        smartbandList.sortByDescending { it.rssi }
                        trySend(smartbandList.toList())
                        if (scanCount > 30) {
                            BleScannerHelper.getInstance().stopScan(context)
                        }
                    }
                }
            }

            override fun onScanFailed(p0: Int) {
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onParsedData(
                p0: BluetoothDevice?,
                p1: ScanRecord?
            ) {
            }

            override fun onBatchScanResults(p0: List<ScanResult?>?) {
            }
        }

        BleScannerHelper.getInstance().scanDevice(context,
            UUID.randomUUID(), callback)

        awaitClose {
            BleScannerHelper.getInstance().stopScan(context)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE : ScannerRepository? = null

        fun getInstance(context: Context) : ScannerRepository {
            return INSTANCE ?: synchronized(this) {
                ScannerRepository(context).also {
                    INSTANCE  = it
                }
            }
        }
    }
}