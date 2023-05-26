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
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.util.concurrent.Executors


abstract class SerialHelper(serialConfig: SerialConfig) : CheckFullFrame {
    private val ACTION_USB_PERMISSION = "com.tmk.libserialhelper.USB_PERMISSION"
    private val onUsbStatusChangeListeners = ArrayList<OnUsbStatusChangeListener>()
    private val onUsbDataListeners = ArrayList<OnUsbDataListener>()
    private var usbManager: UsbManager? = null
    private var serialConfig = SerialConfig()
    private var mContext: Context? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null
    private var connection: UsbDeviceConnection? = null

    // 添加双缓冲，提升效率
    private var mDoubleBuffer: Array<ByteArray?>
    private var mDoubleBufferSize = 0

    // 读取位置，写入位置
    private var writePosition = 0L // 写入位置
    private var readPosition = 0L  // 读取位置

    init {
        this.serialConfig = serialConfig
        mDoubleBuffer = arrayOfNulls(serialConfig.doubleBufferSize)
        mDoubleBufferSize = serialConfig.doubleBufferSize
        initThread()
    }

    private lateinit var mThread: Thread
    private fun initThread() {
        mThread = object : Thread() {
            override fun run() {
                // 释放
                var byteArray = byteArrayOf()
                while (true) {
                    if (readPosition < writePosition) {
//                        // 写入
                        val indexBuffer =
                            mDoubleBuffer[(readPosition++ % mDoubleBufferSize).toInt()] ?: continue
                        val bytes = ByteArray(byteArray.size + indexBuffer.size)

                        // 组合数据
                        byteArray.copyInto(bytes)
                        indexBuffer.copyInto(bytes, byteArray.size)
                        byteArray = bytes
                        // 判断数据完整性, 返回结束和开始索引号
                        val result = isFullFrame(byteArray)
                        if (result.size == 2) {
                            if (result[0] == -1) {
                                // 头没找到，全部数据没用，不严谨，如果头分开了也删除了，几率很小
                                byteArray = byteArrayOf()
                            } else if (result[1] != -1) { // 找到头，也找到尾
                                onUsbDataListeners.forEach {
                                    val temp = byteArray.copyOfRange(result[0], result[1])
//                                    println("收到数据" + Utils.toHexString(temp))
                                    it.onDataReceived(temp)
                                }
                                // 清空byteArray，清除
                                byteArray = byteArray.copyOfRange(result[1], byteArray.size)
                            }
                            // 找到头，未找到尾，需要继续拼接
                        } else if (byteArray.size > serialConfig.dataMaxSize) {
                            byteArray = byteArrayOf()
                        }
                    } else if (byteArray.isNotEmpty()) {
                        // 读取不完
                        val result = isFullFrame(byteArray)
                        if (result.size == 2) {
                            if (result[0] == -1) {
                                // 头没找到，全部数据没用，不严谨，如果头分开了也删除了，几率很小
                                byteArray = byteArrayOf()
                            } else if (result[1] != -1) { // 找到头，也找到尾
                                onUsbDataListeners.forEach {
                                    it.onDataReceived(byteArray.copyOfRange(result[0], result[1]))
                                }
                                // 清空byteArray，清除
                                byteArray = byteArray.copyOfRange(result[1], byteArray.size)
                            }
                        } else {
                            byteArray = byteArrayOf()
                        }
                    }
                    SystemClock.sleep(serialConfig.readInterval)
                }
            }
        }
        mThread.start()
    }

    private val mUsbPermissionActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                onUsbStatusChangeListeners.forEach { it.onUsbDeviceAttached() }
                // 设备连接上，申请权限
                requestPermission()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                onUsbStatusChangeListeners.forEach { it.onUsbDeviceDetached() }
                // 设备拔出，断开连接
                disconnectDevice();
            } else if (ACTION_USB_PERMISSION == action) { // 代表是USB权限
                synchronized(this) {

                    val usbManager = mContext?.getSystemService(Context.USB_SERVICE) as UsbManager

                    val usbDevice = usbManager.deviceList.values.first()
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED,
                            false
                        )
                    ) { // USB权限已授予。  // 用户点击确定，拥有权限
                        onUsbStatusChangeListeners.forEach { it.onPermissionGranted() }
                        if (serialConfig.autoConnect) {
                            // 连接设备
                            connectDevice(usbDevice)
                        }
                    } else {
                        // 权限拒绝
                        onUsbStatusChangeListeners.forEach { it.onPermissionDenied() }
                    }
                }
            }
        }
    }

    private fun requestPermission() {
        val mPermissionIntent =
            PendingIntent.getBroadcast(mContext, 0, Intent(ACTION_USB_PERMISSION), FLAG_IMMUTABLE)
        usbManager?.let {
            for (usbDevice in it.deviceList.values) {
                if (it.hasPermission(usbDevice)) {
                    if (serialConfig.autoConnect) {
                        // 连接设备
                        connectDevice(usbDevice)
                    }
                } else {
                    it.requestPermission(usbDevice, mPermissionIntent)
                }
            }
        }
    }

    fun getAllDevices() = usbManager?.deviceList?.values

    // 连接设备
    fun connectDevice(usbDevice: UsbDevice) {
        // 判断是否支持该设备
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            onUsbStatusChangeListeners.forEach { it.onDeviceNotSupport() }
            return
        }
        // 连接第一个可用的驱动
        val driver = availableDrivers.find { it.device == usbDevice }
        if (driver == null) {
            onUsbStatusChangeListeners.forEach { it.onUsbConnectError(IllegalArgumentException("未找到该设备")) }
            return
        }
        try {
            connection = usbManager?.openDevice(driver.device)
            if (connection == null) {
                requestPermission()
                return
            }
            // 读取端口0数据
            usbSerialPort = driver.ports[0]
            try {
                usbSerialPort?.apply {
                    open(connection)
                    setParameters(
                        serialConfig.baudRate,
                        serialConfig.dataBits,
                        serialConfig.stopBits,
                        serialConfig.parity
                    )
                    dtr = serialConfig.dtr
                    rts = serialConfig.rts
                    onUsbStatusChangeListeners.forEach { it.onUsbConnect(usbDevice) }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                onUsbStatusChangeListeners.forEach { it.onUsbConnectError(e) }
            }

            // 开启数据接收线程
            usbIoManager = SerialInputOutputManager(usbSerialPort, serialManagerListener)
            Executors.newSingleThreadExecutor().submit(usbIoManager!!);
        } catch (e: SecurityException) {
            e.printStackTrace()
            onUsbStatusChangeListeners.forEach { it.onUsbConnectError(e) }
        }
    }

    private val serialManagerListener = object : SerialInputOutputManager.Listener {
        override fun onRunError(e: Exception?) {
            onUsbDataListeners.forEach { it.onDataError(e) }
        }

        override fun onNewData(data: ByteArray?) {
            if (data == null) return
            mDoubleBuffer[(writePosition++ % mDoubleBufferSize).toInt()] = data
        }
    }


    fun writeString(str: String) {
        val data = str.replace(" ", "")
        SerialLog.d("发送字符串数据： $data")
        write(DataConversion.decodeHexString(data))
    }

    fun write(byteArray: ByteArray, retryCount: Int = 0) {
        SerialLog.d("发送字节数组： ${DataConversion.encodeHexString(byteArray)}")
        try {
            usbSerialPort?.write(byteArray, serialConfig.timeout)
        } catch (e: Exception) {
            SerialLog.e("发送数据失败了！！！ ")
            write(byteArray, retryCount + 1)
        }
    }

    // 断开连接
    fun disconnectDevice() {
        usbIoManager?.apply {
            listener = null
            stop()
            usbIoManager = null
        }
        usbSerialPort?.apply {
            try {
                close()
                usbSerialPort = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        connection?.apply {
            close()
            connection = null
        }
        onUsbStatusChangeListeners.forEach { it.onUsbDisconnect() }
    }

    // usb设备插入监听
    fun onCreate(context: Context) {
        mContext = context
        val usbFilter = IntentFilter()
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        usbFilter.addAction(ACTION_USB_PERMISSION)
        context.registerReceiver(mUsbPermissionActionReceiver, usbFilter)
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        // 刚启动
        disconnectDevice()
        // 连接设备
        Handler().postDelayed({
            val devices = getAllDevices()
            if (!devices.isNullOrEmpty()) {
                requestPermission()
            }
        }, 100)
    }

    fun onDestory() {
        mThread.interrupt()
//        mThread.join()
        mContext?.unregisterReceiver(mUsbPermissionActionReceiver)
        disconnectDevice()
        onUsbStatusChangeListeners.clear()
    }

    fun addOnUsbStatusChangeListener(onUsbStatusChangeListener: OnUsbStatusChangeListener) {
        onUsbStatusChangeListeners.add(onUsbStatusChangeListener)
    }

    fun removeOnUsbStatusChangeListener(onUsbStatusChangeListener: OnUsbStatusChangeListener) {
        val index = onUsbStatusChangeListeners.indexOf(onUsbStatusChangeListener)
        if (index != -1) {
            onUsbStatusChangeListeners.removeAt(index)
        }
    }

    fun removeAllUsbStatusChangeListener() {
        onUsbStatusChangeListeners.clear()
    }

    fun addOnUsbDataListener(onUsbDataListener: OnUsbDataListener) {
        onUsbDataListeners.add(onUsbDataListener)
    }

    fun removeOnUsbDataListener(onUsbDataListener: OnUsbDataListener) {
        val index = onUsbDataListeners.indexOf(onUsbDataListener)
        if (index != -1) {
            onUsbDataListeners.removeAt(index)
        }
    }

    fun removeAllUsbDataListener() {
        onUsbStatusChangeListeners.clear()
    }
}
