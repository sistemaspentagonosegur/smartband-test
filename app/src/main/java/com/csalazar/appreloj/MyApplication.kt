package com.csalazar.appreloj

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.csalazar.appreloj.util.startMqttService
import com.oudmon.ble.base.bluetooth.BleAction
import com.oudmon.ble.base.bluetooth.BleOperateManager
import kotlin.properties.Delegates

/**
 * @Author: Hzy
 * @CreateDate: 2021/6/25 11:50
 *
 * "程序应该是写给其他人读的,
 * 让机器来运行它只是一个附带功能"
 */
class MyApplication : Application(){

    var hardwareVersion: String = ""
    var firmwareVersion:String =""

    override fun onCreate() {
        super.onCreate()
        EventRepository.getInstance(this).startListening()
        initBle()
        ScannerRepository.getInstance(this)
    }
    fun  initBle(){
        BleOperateManager.getInstance(this)
        BleOperateManager.getInstance().init()
        val deviceFilter: IntentFilter = BleAction.getDeviceIntentFilter()
        val deviceReceiver = BluetoothReceiver()
        ContextCompat.registerReceiver(
            this,
            deviceReceiver,
            deviceFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        CONTEXT = applicationContext
        startMqttService()
    }



    companion object {
        private var application: Application? = null
        var CONTEXT: Context by Delegates.notNull()
        fun getApplication(): Application? {
            if (application == null) {
                throw RuntimeException("Not support calling this, before create app or after terminate app.")
            }
            return application
        }

        val getInstance: MyApplication by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MyApplication()
        }
    }
}