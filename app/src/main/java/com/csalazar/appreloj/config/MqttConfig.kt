package com.csalazar.appreloj.config

import com.csalazar.appreloj.BuildConfig
data class MqttConfig(
    val brokerUrl: String,
    val username: String,
    val password: String
)

object MqttModule {

    fun getMqttConfig(): MqttConfig = MqttConfig(
        brokerUrl = BuildConfig.MQTT_BROKER_URL,
        username = BuildConfig.MQTT_USER,
        password = BuildConfig.MQTT_PASSWORD
    )
}