package com.csalazar.appreloj

import android.app.Application
import android.content.Context
import com.csalazar.appreloj.domain.Smartband
import com.oudmon.ble.base.bluetooth.BleOperateManager

class SmartbandRepository private constructor(context: Context) {
    private val context = context.applicationContext as Application

    private val sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key),
        Context.MODE_PRIVATE)

        fun  operateManager(): BleOperateManager = BleOperateManager.getInstance(context)

    fun saveSmartband(smartBand: Smartband) {
        sharedPreferences.edit().apply {
            putString(context.getString(R.string.KEY_BLE_NAME), smartBand.deviceName)
            putString(context.getString(R.string.KEY_BLE_ADDRESS), smartBand.deviceAddress)
        }.also {
            it.commit()
        }
    }

    fun getSavedSmartband() : Smartband? {
        val name = sharedPreferences.getString(context.getString(R.string.KEY_BLE_NAME), null)
        val address = sharedPreferences.getString(context.getString(R.string.KEY_BLE_ADDRESS), null)
        if (name != null && address != null) {
            return Smartband(name, address, 0)
        }
        return null
    }

    fun connectSmartband(smartband: Smartband) {
        operateManager().connectDirectly(smartband.deviceAddress)
    }

    fun disconnect() {
        operateManager().disconnect()
    }

    companion object {
        @Volatile
        private var INSTANCE : SmartbandRepository? = null

        fun getInstance(context: Context) : SmartbandRepository {
            return INSTANCE ?: synchronized(this) {
                SmartbandRepository(context).also {
                    INSTANCE  = it
                }
            }
        }
    }
}