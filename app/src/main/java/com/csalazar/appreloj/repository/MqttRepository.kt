package com.csalazar.appreloj.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.csalazar.appreloj.config.MqttConfig
import com.csalazar.appreloj.config.MqttModule
import com.csalazar.appreloj.service.MqttService
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

interface MqttMessageListener {
    fun onMqttMessage(topic: String, payload: String)
}

class MqttRepository private constructor(
     context: Context,
) : MqttCallbackExtended {
    private val context = context.applicationContext as Application
    private val config = MqttModule.getMqttConfig()

    companion object {
        const val QOS_EXACTLY_ONCE = 2
        const val QOS_AT_LEAST_ONCE = 1
        const val QOS_AT_MOST_ONCE = 0

        private const val PREFIX = "pentracker"
        private const val TOPIC_COMMANDS = "$PREFIX/%s/commands"
        private const val TOPIC_LOCATION = "$PREFIX/%s/location"
        private const val TOPIC_STATUS = "$PREFIX/%s/status"
        private const val TOPIC_SOS = "$PREFIX/%s/sos"
        private const val TOPIC_NOTIFICATION = "$PREFIX/notificate/%s/%s"

        private const val TAG = "MqttRepository"

        fun commandsTopic(unityId: Long): String = TOPIC_COMMANDS.format(unityId)
        fun locationTopic(unityId: Long): String = TOPIC_LOCATION.format(unityId)
        fun statusTopic(unityId: Long): String = TOPIC_STATUS.format(unityId)
        fun sosTopic(unityId: Long): String = TOPIC_SOS.format(unityId)

        fun notificationTopic(unityId: Long, userId: String) : String = TOPIC_NOTIFICATION.format(unityId, userId)

        private var INSTANCE : MqttRepository? = null

        fun getInstance(context: Context) : MqttRepository{
            return INSTANCE ?: synchronized(this){
                MqttRepository(context).also {
                    INSTANCE = it
                }
            }
        }
    }

    @Volatile
    private var client: MqttAsyncClient? = null

    @Volatile
    private var connecting = false

    private val subscribedTopics = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val listeners = CopyOnWriteArrayList<MqttMessageListener>()

    private val _connectionState = MutableLiveData(false)
    val connectionState: LiveData<Boolean> = _connectionState

    fun addMessageListener(listener: MqttMessageListener) {
        listeners.add(listener)
    }

    fun removeMessageListener(listener: MqttMessageListener) {
        listeners.remove(listener)
    }

    @Synchronized
    fun connect() {
        if (isConnected() || connecting) return
        connecting = true

        try {
            val localClient = client ?: MqttAsyncClient(
                config.brokerUrl,
                "pentracker-" + UUID.randomUUID().toString(),
                MemoryPersistence()
            ).also { client = it }

            localClient.setCallback(this)
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = false
                connectionTimeout = 30
                keepAliveInterval = 60
                if (config.username.isNotBlank()) {
                    userName = config.username
                    password = config.password.toCharArray()
                }
            }

            localClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    connecting = false
                    _connectionState.postValue(true)
                    Log.d(TAG, "Conectado al broker MQTT")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    connecting = false
                    _connectionState.postValue(false)
                    Log.d(TAG, "Fallo la conexión MQTT")
                }
            })
        } catch (e: MqttException) {
            connecting = false
            Log.d(TAG, "Error al conectar MQTT")
        }
    }

    @Synchronized
    fun disconnect() {
        connectedClient()?.let { localClient ->
            try {
                subscribedTopics.toList().forEach { localClient.unsubscribe(it) }
                subscribedTopics.clear()
                if (localClient.isConnected) {
                    localClient.disconnect()
                }
                localClient.close()
            } catch (e: MqttException) {
                Log.d(TAG, "Error al desconectar MQTT")
            }
        }
        client = null
        connecting = false
        _connectionState.postValue(false)
        Log.d(TAG,"Desconectado del broker MQTT")
        context.stopService(Intent(context, MqttService::class.java))
    }

    fun isConnected(): Boolean = connectedClient()?.isConnected == true

    fun subscribe(unityId: Long, userId: String, qos: Int = QOS_AT_LEAST_ONCE) {
        commandsTopic(unityId).let { topic ->
            subscribedTopics.add(topic)
            subscribeTopic(topic, qos)
        }
        notificationTopic(unityId, userId).let {
                topic ->
            subscribedTopics.add(topic)
            subscribeTopic(topic, qos)
        }
    }

    fun unsubscribe(unityId: Long) {
        commandsTopic(unityId).let { topic ->
            subscribedTopics.remove(topic)
            connectedClient()?.let {
                try {
                    if (it.isConnected) it.unsubscribe(topic)
                } catch (e: MqttException) {
                    Log.d(TAG, "Error al cancelar suscripción $topic")
                }
            }
        }
    }

    fun publish(topic: String, payload: String, qos: Int = QOS_AT_LEAST_ONCE, retained: Boolean = false) {
        connectedClient()?.let {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                isRetained = retained
            }
            try {
                it.publish(topic, message)
            } catch (e: MqttException) {
                Log.d(TAG, "Error al publicar en $topic")
            }
        }
    }

    fun publishLocation(unityId: Long, payload: String, retained: Boolean = true) {
        publish(locationTopic(unityId), payload, QOS_AT_LEAST_ONCE, retained)
    }

    fun publishStatus(unityId: Long, payload: String, retained: Boolean = true) {
        publish(statusTopic(unityId), payload, QOS_AT_LEAST_ONCE, retained)
    }

    fun publishSos(unityId: Long, payload: String, retained: Boolean = false) {
        publish(sosTopic(unityId), payload, QOS_AT_LEAST_ONCE, retained)
    }

    private fun connectedClient(): MqttAsyncClient? = client?.takeIf { it.isConnected }

    private fun subscribeTopic(topic: String, qos: Int) {
        connectedClient()?.let {
            try {
                it.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG,"Suscrito a $topic")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(TAG, "Fallo suscripción a $topic")
                    }
                })
            } catch (e: MqttException) {
                Log.d(TAG, "Error al suscribirse a $topic")
            }
        }
    }

    private fun notifyListeners(topic: String, payload: String) {
        listeners.forEach {
            try {
                it.onMqttMessage(topic, payload)
            } catch (e: Exception) {
                Log.d(TAG, "Error en listener MQTT")
            }
        }
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d(TAG,"Conexión ${if (reconnect) "reconectada" else "completada"}: $serverURI")
        _connectionState.postValue(true)
        subscribedTopics.forEach { topic ->
            subscribeTopic(topic, QOS_AT_LEAST_ONCE)
        }
    }

    override fun connectionLost(cause: Throwable?) {
        Log.d(TAG, "Conexión MQTT perdida")
        _connectionState.postValue(false)
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        val payload = String(message.payload, Charsets.UTF_8)
        Log.d(TAG,"Mensaje recibido en $topic: $payload")
        notifyListeners(topic, payload)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        // no-op
    }
}