package com.tmk.libserialhelper

/**
 * 串口数据监听器
 */
interface OnUsbDataListener {
    /**
     * @param bytes 接收串口返回的数据，在子线程中
     */
    fun onDataReceived(bytes: ByteArray)
    fun onDataError(e: Exception?)
}
