package com.tmk.libserialhelper

import android.hardware.usb.UsbDevice

/**
 * 串口状态变化的监听器
 */
interface OnUsbStatusChangeListener {
    fun onUsbDeviceAttached()
    fun onUsbDeviceDetached()
    fun onPermissionGranted()
    fun onPermissionDenied()
    fun onDeviceNotSupport()
    fun onUsbConnect(usbDevice: UsbDevice)
    fun onUsbConnectError(e: Exception)
    fun onUsbDisconnect()
}
