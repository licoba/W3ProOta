package com.licoba.w3pro0ta

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.PopTip
import com.licoba.w3pro0ta.databinding.ActivityMainBinding
import com.licoba.w3pro0ta.ui.theme.W3ProOTATheme
import com.sjx.serialhelperlibrary.DataConversion.decodeHexString
import com.sjx.serialhelperlibrary.DataConversion.encodeHexString
import com.sjx.serialhelperlibrary.OnUsbDataListener
import com.sjx.serialhelperlibrary.OnUsbStatusChangeListener
import com.sjx.serialhelperlibrary.SerialConfig
import com.sjx.serialhelperlibrary.SerialHelper
import com.sjx.serialhelperlibrary.W3ProCMD
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    companion object {
        const val TAG = "😁"
    }

    private lateinit var serialHelper: SerialHelper

    private lateinit var mBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(mBinding.root)
        initSerialPort()
        mBinding.btnInitPort.setOnClickListener { initSerialPort() }
        mBinding.btnSendData.setOnClickListener { sendData() }
    }


    private fun sendData() {
        serialHelper.write(decodeHexString(W3ProCMD.START_UPD.hexContent))
    }


    private fun initSerialPort() {
        LogUtils.d(TAG, "initSerialPort: 开始")
        // 串口配置
        val serialConfig = SerialConfig()
        serialConfig.autoConnect = true // 默认连接第一个
        serialConfig.baudRate = 115200 // 串口波特率
        serialConfig.readInterval = 10 // ms，子线程读取，休眠间隔，双缓冲读取与写速率调整，默认10ms
        serialConfig.doubleBufferSize = 20 // 双缓冲容量大小，循环覆盖缓存
        serialConfig.dataMaxSize = 30000  // 当拼接数据未找到完整帧，长度大于30000清空，可根据实际情况适当调整
        serialHelper = object : SerialHelper(serialConfig) {
            override fun isFullFrame(data: ByteArray): IntArray {
                // 子线程 根据自己的完整帧判断方式 返回数据的起始索引和结束索引
                // 示例中有ByteUtils工具类，查找帧头帧尾的索引号
                // ByteUtils.getIndexRange(data, startBytes, endBytes)
                return intArrayOf(0, data.size)
            }
        }
        serialHelper.onCreate(this)
        LogUtils.d(TAG, "initSerialPort: 结束")
        serialHelper.addOnUsbStatusChangeListener(mUsbStatusChangeListener)
        serialHelper.addOnUsbDataListener(mUsbDataListener)
        mBinding.btnInitPort.isEnabled = false
        PopTip.show("串口初始化成功");


    }

    val mUsbStatusChangeListener = object : OnUsbStatusChangeListener {
        override fun onUsbDeviceAttached() {
            LogUtils.i("StatusChange", "onUsbDeviceAttached")
        }

        override fun onUsbDeviceDetached() {
            LogUtils.i("StatusChange", "onUsbDeviceDetached")
        }

        override fun onPermissionGranted() {
            LogUtils.i("StatusChange", "onPermissionGranted")
        }

        override fun onPermissionDenied() {
            LogUtils.i("StatusChange", "onPermissionDenied")
        }

        override fun onDeviceNotSupport() {
            LogUtils.i("StatusChange", "onDeviceNotSupport")
        }

        override fun onUsbConnect(usbDevice: UsbDevice) {
            LogUtils.i("StatusChange", "onUsbConnect")
        }

        override fun onUsbConnectError(e: Exception) {
            LogUtils.i("StatusChange", "onUsbConnectError")
        }

        override fun onUsbDisconnect() {
            LogUtils.i("StatusChange", "onUsbDisconnect")
        }
    }


    // 数据监听

    val mUsbDataListener = object : OnUsbDataListener {
        override fun onDataError(e: Exception?) {
            // 数据异常
            LogUtils.d("数据监听器", "数据异常 $e")

        }

        override fun onDataReceived(bytes: ByteArray) {
            // 处理返回的数据, 当前线程为子线程
            runOnUiThread {
                processReceivedData(bytes)
            }
        }
    }


    private fun processReceivedData(bytes: ByteArray) {
        // 注意：contentToString是十六进制数据
        LogUtils.d("数据监听器", "收到了数据  ${encodeHexString(bytes)}")
        if (bytes.decodeToString() == W3ProCMD.RECEIVE_START.content) {
            PopTip.show("蓝汛已收到开始UPD升级指令");
        } else if (encodeHexString(bytes) == W3ProCMD.WAIT_READING_DATA.hexContent) { // 比较十六进制是否一致
            LogUtils.d("蓝汛等待发送升级包数据...")
            PopTip.show("蓝汛等待发送升级包数据...")
            sendUpdData(bytes)
        }
    }


    fun sendUpdData(receiveBytes: ByteArray) {
        lifecycleScope.launch {
            delay(5)
            val context = this@MainActivity
            val fileName = "fw5000_1.upd"
            val byteArray = FileUtil.readBytesFromAsset(context, fileName, 0, 512)
            byteArray?.let {
                serialHelper.write(byteArray)
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        serialHelper.onDestory()

    }
}

