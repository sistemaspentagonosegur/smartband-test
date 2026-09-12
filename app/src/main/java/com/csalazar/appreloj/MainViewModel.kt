package com.csalazar.appreloj

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.csalazar.appreloj.domain.SmartWatch
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(application: Application) : AndroidViewModel(application), ScanWrapperCallback {
    var scanCount : Int = 0
    private val _patrolList = MutableStateFlow<List<SmartWatch>>(emptyList())
    val patrolList: StateFlow<List<SmartWatch>> = _patrolList.asStateFlow()

    override fun onStart() {
        Log.d("BLUECS", "ONSTART")
    }

    override fun onStop() {
        Log.d("BLUECS", "ONSTOP")
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

            if (!patrolList.value.contains(smartWatch)) {
                scanCount++
                _patrolList.update { currentList ->
                    val listNuevo = listOf(smartWatch, *currentList.toTypedArray())
                    listNuevo.sortedByDescending { it.rssi }
                }

                if (scanCount > 30) {
                    BleScannerHelper.getInstance().stopScan(this)
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