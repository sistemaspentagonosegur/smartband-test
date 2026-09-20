package com.csalazar.appreloj.domain

data class Smartband (
    val deviceName:String,
    val deviceAddress:String?,
    val rssi:Int,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Smartband

        if (deviceName != other.deviceName) return false
        if (deviceAddress != other.deviceAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceName.hashCode()
        result = 31 * result + (deviceAddress?.hashCode() ?: 0)
        return result
    }
}
