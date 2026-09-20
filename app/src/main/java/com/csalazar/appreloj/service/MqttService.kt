package com.csalazar.appreloj.service

import android.app.Application
import androidx.core.app.ServiceCompat
import com.csalazar.appreloj.MainActivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothStatusCodes
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.csalazar.appreloj.EventRepository
import com.csalazar.appreloj.ScannerRepository
import com.csalazar.appreloj.SmartbandRepository
import com.csalazar.appreloj.repository.MqttRepository
import com.csalazar.appreloj.util.startMqttService
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.spp.RtkSppConstants
import com.oudmon.ble.base.communication.CommandHandle
import com.oudmon.ble.base.communication.Constants
import com.oudmon.ble.base.communication.req.SimpleKeyReq
import com.oudmon.ble.base.util.BluetoothUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MqttService : Service() {

    companion object {
        const val TAG = "MqttService"
        const val ACTION_STOP = "pe.pentagonosegur.pentracker1.action.MQTT_STOP"
        private const val CHANNEL_ID = "CANAL_MQTT"
        private const val CHANNEL_NAME = "Conexión MQTT"
        private const val NOTIFICATION_ID = 9001
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val backoff = listOf(15.seconds, 30.seconds, 1.minutes, 2.minutes, 5.minutes, 10.minutes, 15.minutes, 20.minutes)
    private var watchdogIntentos = 0
    private var watchdogActivo = false
    private var pausaBateria = false               // "el band está lejos/apagado"


    override fun onCreate() {
        super.onCreate()
        observarBridge()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            SmartbandRepository.getInstance(applicationContext).disconnect()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startMqtt(intent)
        ensureConnected()
        return START_STICKY
    }

    private fun startMqtt(intent: Intent?) {
        createChannel()

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Conexión activa")
            .setContentText("Servicio MQTT en ejecución")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val repository = MqttRepository.getInstance(applicationContext)
        repository.connect()
    }

    fun ensureConnected()  {
        val bleManager = BleOperateManager.getInstance(applicationContext as Application)
        if (bleManager.isConnected || bleManager.connectState == RtkSppConstants.STATE_DEVICE_CONNECTING) return
        val device =
            SmartbandRepository.getInstance(applicationContext).getSavedSmartband() ?: return
        bleManager.connectDirectly(device.deviceAddress)
        Log.d(TAG, "Intento " + watchdogIntentos)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la conexión MQTT"
            }

            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)

        }
    }


    private fun observarBridge() {
        val eventRepo = EventRepository.getInstance(applicationContext)

        serviceScope.launch {
            eventRepo.bluetoothEvents.collect{
                if (it.connected) {
                    watchdogIntentos = 0

                    serviceScope.launch {
                        delay(1000.milliseconds)
                        CommandHandle.getInstance()
                            .executeReqCmdNoCallback(
                                SimpleKeyReq(Constants.CMD_BIND_SUCCESS)
                            )
                    }
                } else  {
                    Log.d(TAG, "Programando watchdog")
                    programarWatcchdog()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        SmartbandRepository.getInstance(applicationContext).disconnect()
        MqttRepository.getInstance(applicationContext).disconnect()

    }


    private fun programarWatcchdog() {
        val bleManager = BleOperateManager.getInstance(applicationContext as Application)

        if (watchdogActivo || pausaBateria) return
        watchdogActivo = true
        serviceScope.launch {
            delay(15.seconds)
            var paso = 0
            while (isActive) {
                if (debePausar()) break
                if (bleManager.isConnected) break
                if (bleManager.connectState == RtkSppConstants.STATE_DEVICE_CONNECTING) {
                    delay(10.seconds); continue
                }
                ensureConnected()
                delay(15.seconds)
                if (bleManager.isConnected) break
                watchdogIntentos++
                if (watchdogIntentos >= backoff.size) { //pausar(); break
                    }      // PAUSA POR BATERÍA
                paso = paso.coerceAtMost(backoff.lastIndex)
                delay(backoff[paso])
                paso++
            }
            watchdogActivo = false
        }
    }

    private fun pausar() {
        pausaBateria = true
    }

    private fun debePausar() =
        pausaBateria || !BluetoothUtils.isEnabledBluetooth(this) || SmartbandRepository.getInstance(applicationContext).getSavedSmartband() == null



    override fun onBind(intent: Intent?): IBinder? = null
}