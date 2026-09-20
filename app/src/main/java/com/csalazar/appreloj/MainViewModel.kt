package com.csalazar.appreloj

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csalazar.appreloj.domain.Smartband
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = ScannerRepository.getInstance(application)
    private val smartbandRepository = SmartbandRepository.getInstance(application)

    private val _devices = MutableStateFlow<List<Smartband>>(emptyList())
    val devices: StateFlow<List<Smartband>> = _devices.asStateFlow()

    private val _bluetoothEvents = MutableStateFlow<List<BluetoothConnectionEvent>>(emptyList())

    val bluetoothEvents = _bluetoothEvents.asStateFlow()

    private val bleRepository = EventRepository.getInstance(application)


    init {
        viewModelScope.launch {
            bleRepository.bluetoothEvents.collect { event ->
                handleBleEvent(event)
            }
        }
    }

    private fun handleBleEvent(event: BluetoothConnectionEvent) {
        _bluetoothEvents.update {
            old ->
                old.toMutableList().apply {
                    add(event)
                }
        }
    }
    private var scanJob : Job? = null

    fun startScanning() {
        scanJob ?.cancel()
        scanJob = viewModelScope.launch {
            scanner.scanDevices().collect { updatedList ->
                _devices.value = updatedList
            }
        }
    }

    fun connectDevice(smartband: Smartband) {
        smartbandRepository.connectSmartband(smartband)
    }
}