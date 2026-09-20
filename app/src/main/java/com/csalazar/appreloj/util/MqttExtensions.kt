package com.csalazar.appreloj.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.csalazar.appreloj.service.MqttService

fun Context.startMqttService() {
    val intent = Intent(this, MqttService::class.java).apply {

    }
    ContextCompat.startForegroundService(this, intent)
}

fun Context.stopMqttService() {
    stopService(Intent(this, MqttService::class.java))
}