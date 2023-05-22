package com.sjx.serialhelperlibrary

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

enum class W3ProCMD(val content: String,val hexContent:String) {
    // 客户端发送的指令
    START_UPD("START_UPD^_^","53544152545f5550445e5f5e"),

    // 收到的响应指令
    RECEIVE_START("RECEIVESTART","524543454956455354415254"),  // 蓝汛收到了我们发送的START_UPD指令
    WAIT_READING_DATA("WAIT_READING_DATA","AA550200000000000002000003010000") , //蓝汛向我们发送的读取数据指令，告诉我们可以发送数据给他了
}
