package com.csalazar.appreloj

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.csalazar.appreloj.domain.SmartWatch
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback

class SmartWatchScanner(
    private val context: Context
) : ScanWrapperCallback  {
    private var scanCount : Int = 0
    private val smartWatchList = mutableListOf<SmartWatch>()

    override fun onStart() {
        Log.d("SmartWatchScanner", "onStartScanning")
    }

    override fun onStop() {
        Log.d("SmartWatchScanner", "onStopScanning")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onLeScan(
        device: BluetoothDevice?,
        rssi: Int,
        p2: ByteArray?
    ) {
        if (device != null && (!device.name.isNullOrEmpty())) {
//                if (device.name.startsWith("O_")||device.name.startsWith("Q_")) {
//
//                }

            val smartWatch = SmartWatch(device.name, device.address, rssi)
            Log.i("1111",device.name+"---"+ device.address)

            if (!smartWatchList.contains(smartWatch)) {
                scanCount++
                smartWatchList.add(0, smartWatch)
                smartWatchList.sortByDescending { it.rssi }
                if (scanCount > 30) {
                    BleScannerHelper.getInstance().stopScan(context)
                }
            }
        }
    }

    override fun onScanFailed(p0: Int) {
        Log.d("BLUECS", "$p0")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onParsedData(
        p0: BluetoothDevice?,
        p1: ScanRecord?
    ) {
        Log.d("BLUECS", "onParsedData ${p0?.bluetoothClass}")
    }

    override fun onBatchScanResults(p0: List<ScanResult?>?) {
        Log.d("BLUECS", "onBatchScanResults")
    }
}