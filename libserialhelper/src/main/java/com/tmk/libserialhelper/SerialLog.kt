package com.tmk.libserialhelper

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors

object SerialLog {
    val TAG = "SerialLog"

    var openDebug = true

    fun d(msg: String) {
        if (openDebug) {
            Log.d(TAG, msg)
        }
    }

    fun e(msg: String) {
        if (openDebug) {
            Log.e(TAG, msg)
        }
    }
}
