package com.licoba.w3pro0ta

import android.hardware.usb.UsbDevice
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener
import com.licoba.w3pro0ta.MyUtil.getCheckSum
import com.licoba.w3pro0ta.databinding.ActivityMainBinding
import com.tmk.libserialhelper.DataConversion.decodeHexString
import com.tmk.libserialhelper.DataConversion.encodeHexString
import com.tmk.libserialhelper.OnUsbDataListener
import com.tmk.libserialhelper.OnUsbStatusChangeListener
import com.tmk.libserialhelper.SerialConfig
import com.tmk.libserialhelper.SerialHelper
import com.tmk.libserialhelper.W3ProCMD
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {


    companion object {
        const val TAG = "😁"
    }

    private lateinit var serialHelper: SerialHelper

    private lateinit var mBinding: ActivityMainBinding
    private var mUpdFileName: String = "fw5000_1.upd"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(mBinding.root)
        initThirdLib()
        initSerialPort()
        mBinding.btnInitPort.setOnClickListener { initSerialPort() }
        mBinding.btnSendData.setOnClickListener { sendData() }
        mBinding.btnSendFirstCmd.setOnClickListener { sendFirstData() }
        mBinding.btnSendFirstFileData.setOnClickListener { sendFirstFileData() }
        mBinding.btnChooseUpdFile.setOnClickListener { showChooseFilePop() }
    }

    private fun initThirdLib() {
        DialogX.DEBUGMODE = false
        val mConfig = LogUtils.getConfig()
        mConfig.apply {
            globalTag = TAG
            isLogHeadSwitch = false
            setBorderSwitch(false)
        }
    }

    private fun showChooseFilePop() {
        BottomMenu.show(arrayOf("fwq5000_1.upd", "fw5000_2.upd"))
            .setMessage("选择一个升级文件").onMenuItemClickListener =
            OnMenuItemClickListener { _, text, _ ->
                mUpdFileName = text.toString()
                false
            }
    }


    private fun sendData() {
        serialHelper.write(decodeHexString(W3ProCMD.START_UPD.hexContent))
    }


    private fun sendFirstData() {
        val data = "AA 55 02 00 00 00 00 00 9A 23 00 00 BE 01 00 00"
        serialHelper.writeString(data)
    }

    private fun sendFirstFileData() {
        // 第一包文件的数据
        val data =
            "55504401F8470A00C9CEC6CF14000000534D415401000000C08C518C91BA000000000000C4C5D6000C000000100000000101000100500000CBC5D9000800000000000084C7A703C8D5D0C3001C000000010000000000000000000000000000000000000000B6000000020000C4C1D4C14C000000000200000034000000DA00000010080000B80000000A000000D400000006000000360000004000000076000000100000D1BE000000C20000001200000030010000860000003000000000060000DA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000EA0800425C01000000D48F"
        serialHelper.writeString(data)
    }


    private fun initSerialPort() {
        LogUtils.d("initSerialPort: 开始")
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
        LogUtils.d("initSerialPort: 结束")
        serialHelper.addOnUsbStatusChangeListener(mUsbStatusChangeListener)
        serialHelper.addOnUsbDataListener(mUsbDataListener)
        mBinding.btnInitPort.isEnabled = false
        PopTip.show("串口初始化成功");

    }

    private val mUsbStatusChangeListener = object : OnUsbStatusChangeListener {
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

    private val mUsbDataListener = object : OnUsbDataListener {
        override fun onDataError(e: Exception?) {
            // 数据异常
            LogUtils.d("数据异常 $e")

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
        LogUtils.d("收到了数据  ${encodeHexString(bytes)}")
        if (bytes.decodeToString() == W3ProCMD.RECEIVE_START.content) {
            LogUtils.d("蓝汛已收到开始UPD升级指令...")
//            WaitDialog.show("准备升级...");
//            PopTip.show("蓝汛已收到开始UPD升级指令")
        } else if (isWaitingDataPkg(bytes)) {
            LogUtils.d("蓝汛等待发送升级包数据...")
//            PopTip.show("蓝汛等待发送升级包数据...")
            WaitDialog.show("正在升级中...");
            sendUpdData(bytes)
        } else if (isCheckUartPkg(bytes)) {  // 直接原封不动返回这个包即可
            LogUtils.d("蓝汛等待回复确认为upd模式...")
            PopTip.show("蓝汛等待回复确认为upd模式...")
            serialHelper.write(bytes)
        } else if (isUpdFinishPkg(bytes)) {
            LogUtils.d("😁升级完成！！！")
            TipDialog.show("😁升级完成!", WaitDialog.TYPE.SUCCESS);
        } else if (isCommunicationPkg(bytes)) {
            LogUtils.d("是串口通信协议的数据包")
            parseProtocolData(bytes)
        }
    }

    private fun parseProtocolData(byteArray: ByteArray) {
        val head = byteArray.sliceArray(0..2).toUByteArray()
        val cmd = byteArray[3]
        val length = byteArray[4]
        val data = W3PacketData(
            result = byteArray[5],
            mode = byteArray[6],
            headsetRole = byteArray[7],
            electric = byteArray[8],
            voltage = ByteBuffer.wrap(byteArray.sliceArray(9..10)).short,
            lanXunFirmV = ByteBuffer.wrap(byteArray.sliceArray(11..12)).short,
            bleMac = byteArray.sliceArray(13..18).toUByteArray(),
            bleFirmV = ByteBuffer.wrap(byteArray.sliceArray(19..20)).short,
        )
        val check = byteArray.last()

        val w3Packet = W3TotalPacket(
            head = head,
            cmd = cmd,
            dataLength = length,
            data = data,
            check = check
        )

        LogUtils.d("解析后的数据：$w3Packet")
        LogUtils.d(
            "解析后的数据sum校验和（Kotlin计算的16进制结果）：${
                crc8Maxim(
                    byteArray,
                    byteArray.size - 1
                ).toHex().uppercase()
            }"
        )

    }


    /**
     * 判断是不是等待我们发送数据的包，
     * 有一个特征就是以SIGN:55AA CMD:02 开头
     */
    private fun isWaitingDataPkg(bytes: ByteArray): Boolean {
        return encodeHexString(bytes).uppercase().startsWith("AA5502")
    }


    /**
     * 是否是检查Upd模式的包，如果是，就按照文档，直接返回就行了
     */
    private fun isCheckUartPkg(bytes: ByteArray): Boolean {
        return encodeHexString(bytes).uppercase().startsWith("AA5501")
    }


    /**
     * 是否是升级完成的包，如果是，那么提示用户就行了
     */
    private fun isUpdFinishPkg(bytes: ByteArray): Boolean {
        return encodeHexString(bytes).uppercase().startsWith("AA5503")
    }


    /**
     * 是否是通讯的包，都以55 AA FF开头
     */
    private fun isCommunicationPkg(bytes: ByteArray): Boolean {
        return encodeHexString(bytes).uppercase().startsWith("55AAFF")
    }


    fun sendUpdData(bytes: ByteArray) {
        lifecycleScope.launch {
            delay(5)
//            val rxCmd = UartUpdMRxCmd().apply { parseSelf(bytes) }  // 接收的指令包
            val txCmd = UartUpdMTxCmd().apply { parseSelf(bytes) }  // 发送的指令包
            // 获取512字节的文件
            val context = this@MainActivity
            LogUtils.d("解析后的txCmd： ${txCmd.printString()}")
            val byteArrayFile =
                MyUtil.readBytesFromAsset(context, mUpdFileName, txCmd.addr.toInt(), 512)
            byteArrayFile?.let {
                // 首先是512固件包的数据求和
                txCmd.data_crc = getCheckSum(byteArrayFile, 512)
                // 然后是指令求和
                val sum = getCheckSum(txCmd.toByteArray(), 12)
                txCmd.crc = sum.toUShort()
                serialHelper.write(txCmd.toByteArray())
                serialHelper.write(it)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        serialHelper.onDestory()

    }
}

