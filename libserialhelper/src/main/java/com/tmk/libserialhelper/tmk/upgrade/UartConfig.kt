package com.tmk.libserialhelper.tmk.upgrade

import com.hoho.android.usbserial.driver.UsbSerialPort

data class UartConfig(
    var otaData: ByteArray = byteArrayOf(), //  OTA升级的文件数据 （从文件里读取字节流了传入）
    var openLog:Boolean = true   // 是否打开日志打印
) {
    override fun toString() = buildString {
        append("UartConfig(")
        append("otaData= ${otaData.size}Byte, ")
        append("openLog=${openLog}, ")
        append(")")
    }
}