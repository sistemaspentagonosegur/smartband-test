package com.csalazar.appreloj

import android.content.Context
import com.csalazar.appreloj.domain.Smartband
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EventRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _bluetoothEvents = MutableSharedFlow<BluetoothConnectionEvent>(extraBufferCapacity = 64)
    val bluetoothEvents: SharedFlow<BluetoothConnectionEvent> = _bluetoothEvents.asSharedFlow()
    private val bluetoothReceiver = MyBluetoothReceiver(appContext)
    fun startListening() {
        applicationScope.launch {
            bluetoothReceiver.receive().collect { event ->
                _bluetoothEvents.emit(event)
            }
        }
    }

    fun emitEvent(event: BluetoothConnectionEvent) {
        _bluetoothEvents.tryEmit(event)
    }

    companion object {
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getInstance(context: Context): EventRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

class BluetoothConnectionEvent(
    val connected : Boolean,
    val device: Smartband? = null
)

class BluetoothStatusEvent(
    val enabled : Boolean
)