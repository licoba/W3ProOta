package com.licoba.w3pro0ta

import SerialUtil.crc8Maxim
import SerialUtil.getCheckSum
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener
import com.licoba.w3pro0ta.MyUtil.readBytesFromAssets
import com.licoba.w3pro0ta.databinding.ActivityMainBinding
import com.tmk.libserialhelper.DataConversion.decodeHexString
import com.tmk.libserialhelper.DataConversion.encodeHexString
import com.tmk.libserialhelper.OnUsbDataListener
import com.tmk.libserialhelper.OnUsbStatusChangeListener
import com.tmk.libserialhelper.SerialConfig
import com.tmk.libserialhelper.SerialHelper
import com.tmk.libserialhelper.tmk.UartUpdMTxCmd
import com.tmk.libserialhelper.tmk.W3PacketData
import com.tmk.libserialhelper.tmk.W3ProSendCmd
import com.tmk.libserialhelper.tmk.W3ProUpgradeCMD
import com.tmk.libserialhelper.tmk.W3SendPacket
import com.tmk.libserialhelper.tmk.W3TotalPacket
import com.tmk.libserialhelper.tmk.buildW3ProCmdPkg
import com.tmk.libserialhelper.tmk.upgrade.UartConfig
import com.tmk.libserialhelper.tmk.upgrade.UartError
import com.tmk.libserialhelper.tmk.upgrade.UartOtaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import toHex
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {


    companion object {
        const val TAG = "😁"
    }

    private lateinit var context: Context
    private lateinit var serialHelper: SerialHelper
    private lateinit var mBinding: ActivityMainBinding
    private var mUpdFileName: String = "fw5000_1.upd"
    private var reqDialog: WaitDialog? = null
    private var startDialog: WaitDialog? = null
    private var otaManager: UartOtaManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this@MainActivity
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initThirdLib()
        initSerialPort()
        mBinding.btnInitPort.setOnClickListener { initSerialPort() }
        mBinding.btnSendFirstCmd.setOnClickListener { sendFirstData() }
        mBinding.btnSendFirstFileData.setOnClickListener { sendFirstFileData() }
        mBinding.btnChooseUpdFile.setOnClickListener { showChooseFilePop() }
        mBinding.ivClear.setOnClickListener { mBinding.tvLog.text = "" }
        mBinding.tvLog.movementMethod = ScrollingMovementMethod.getInstance()
        mBinding.btnChooseCmd.setOnClickListener { showChooseCmdPop() }
        mBinding.btnTestOtaSdk.setOnClickListener {
            if (otaManager == null) {
                otaManager = UartOtaManager.getInstance(context, serialHelper)
            }
            val config = UartConfig().apply {
                otaData = readBytesFromAssets(context, mUpdFileName)
            }
            otaManager?.config = config
            otaManager?.listener = mUartEventListener
            otaManager?.startOTA()
        }
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
        BottomMenu.show(arrayOf("fw5000_1.upd", "fw5000_2.upd", "从文件管理器选择"))
            .setMessage("选择一个升级文件").onMenuItemClickListener =
            OnMenuItemClickListener { _, text, index ->
                if (index == 2) PopTip.show("还没做！")
                else mUpdFileName = text.toString()
                false
            }
    }

    private fun showChooseCmdPop() {
        BottomMenu.show(arrayOf("查询耳机角色", "XXX"))
            .setMessage("选择一个指令").onMenuItemClickListener =
            OnMenuItemClickListener { _, text, index ->
                var pkg: W3SendPacket? = null
                pkg = when (text) {
                    "查询耳机角色" -> buildW3ProCmdPkg(W3ProSendCmd.QueryRole)
                    "XXX" -> buildW3ProCmdPkg(W3ProSendCmd.XXX)
                    else -> null
                }
                pkg?.let { sendData(pkg.toByteArray()) }
                false
            }
    }


    private fun sendFirstData() {
        val data = "AA 55 02 00 00 00 00 00 9A 23 00 00 BE 01 00 00"
        sendData(data)
    }


    private fun sendData(bytes: ByteArray) {
        addText("发->${encodeHexString(bytes)}")
        serialHelper.write(bytes)
    }

    private fun sendData(str: String) {
        addText("收<-${str}")
        serialHelper.writeString(str)
    }


    private fun sendFirstFileData() {
        // 第一包文件的数据
        val data =
            "55504401F8470A00C9CEC6CF14000000534D415401000000C08C518C91BA000000000000C4C5D6000C000000100000000101000100500000CBC5D9000800000000000084C7A703C8D5D0C3001C000000010000000000000000000000000000000000000000B6000000020000C4C1D4C14C000000000200000034000000DA00000010080000B80000000A000000D400000006000000360000004000000076000000100000D1BE000000C20000001200000030010000860000003000000000060000DA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000EA0800425C01000000D48F"
        sendData(data)
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

    }

    private val mUsbStatusChangeListener = object : OnUsbStatusChangeListener {
        override fun onUsbDeviceAttached() {
            addText("状态监听：USB设备已插入！")
            LogUtils.i("StatusChange", "onUsbDeviceAttached")
        }

        override fun onUsbDeviceDetached() {
            addText("状态监听：USB设备已拔出！");
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
            addText("状态监听：串口连接成功！")
            LogUtils.i("StatusChange", "onUsbConnect")
            mBinding.tvConnectStatus.text = "已连接"
        }

        override fun onUsbConnectError(e: Exception) {
            LogUtils.i("StatusChange", "onUsbConnectError")
        }

        override fun onUsbDisconnect() {
            addText("状态监听：串口已断开连接！");
            LogUtils.i("StatusChange", "onUsbDisconnect")
            mBinding.tvConnectStatus.text = "未连接"

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
                correctionByteData(bytes)
                processReceivedData(bytes)
            }
        }
    }


    private val mUartEventListener = object : UartOtaManager.UartEventListener {
        override fun onOtaPrepare() {
            WaitDialog.show("准备中...")
        }

        override fun onOtaStart() {
            if (startDialog == null) {
                startDialog = WaitDialog.show("升级中...")
            }
        }

        override fun onOtaProgress(progress: Float) {
            LogUtils.d("升级进度更新： $progress")
            val str = (progress * 100).toInt()
            WaitDialog.show("升级中...($str%)")

        }

        override fun onOtaStop() {
        }

        override fun onOtaFinish() {
            TipDialog.show("升级完成！", WaitDialog.TYPE.SUCCESS)
        }

        override fun onOtaPause() {
        }

        override fun onOtaContinue() {
        }

        override fun onOtaError(err: UartError) {
            TipDialog.show("升级失败！\n ${err.errDesc}", WaitDialog.TYPE.ERROR)
        }

    }


    /***
     * 处理收到的数据（只有数据合法才处理）
     */
    private fun processReceivedData(bytes: ByteArray) {
        // 注意：contentToString是十六进制数据
        LogUtils.d("收到了数据  ${encodeHexString(bytes)}")
        if (isCommunicationPkg(bytes)) {
            LogUtils.d("是串口通信协议的数据包")
            addText("是串口通信协议的数据包")
            parseProtocolData(bytes)
        }
    }

    private fun parseProtocolData(byteArray: ByteArray) {
        addText("处理串口通信协议数据...")
        try {
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

            addText("解析后的数据：$w3Packet")
            LogUtils.d("解析后的数据：$w3Packet")
            LogUtils.d(
                "解析后的数据sum校验和（Kotlin计算的16进制结果）：${
                    crc8Maxim(
                        byteArray,
                        byteArray.size - 1
                    ).toHex().uppercase()
                }"
            )
        } catch (e: Exception) {
            // 解析失败的原因：
            // 1、数据不是合法的协议格式
            // 2、发送的数据不对，没有任何含义，串口端原封不动地返回了数据
            // 3、还没兼容好数据解析的逻辑（自己的解析的代码有问题）
            LogUtils.d("解析失败了！ $e")
            addText("err: 数据解析失败！！！请检查数据格式/解析方法")
        }


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
    private fun isUpdSuccessPkg(bytes: ByteArray): Boolean {
        return encodeHexString(bytes).uppercase().startsWith("AA5503FF")
    }


    /**
     * 是升级失败的包
     */
    private fun isUpdFailPkg(bytes: ByteArray): Boolean {
        // 只有AA5503FF才是升级成功，AA550306就是升级失败了
        val str = encodeHexString(bytes).uppercase()
        return str.startsWith("AA5503") && !str.startsWith("AA5503FF")
    }


    /**
     * 纠正数据
     * 串口的第一个字符有可能变成D5（未知原因导致）但是check又是正常的，也就是串口通信有一定的不确定性
     * 本来应该收到 55AAFF00010064
     * 但是却收到了 D5AAFF00010064
     * 所以需要把 D5修正为55
     */
    private fun correctionByteData(byteArray: ByteArray): ByteArray {
        if (!isHeadValid(byteArray)) return byteArray
        byteArray[0] = 0x55.toByte()
        return byteArray
    }


    /**
     * 判断head是否合法
     */
    private fun isHeadValid(byteArray: ByteArray): Boolean {
        if (byteArray.size < 3) return false
        // 以55AAFF开头
        if (byteArray[0] == 0x55.toByte() && byteArray[1] == 0xAA.toByte() && byteArray[2] == 0xFF.toByte()) return true
        else if (byteArray[0] == 0xD5.toByte() && byteArray[1] == 0xAA.toByte() && byteArray[2] == 0xFF.toByte()) return true
        return false
    }

    /**
     * 是否是通讯的包，都以55 AA FF开头
     */
    private fun isCommunicationPkg(byteArray: ByteArray): Boolean {
        val hexStr = encodeHexString(byteArray).uppercase()
        if (hexStr.length < 8) return false  // 长度不够，肯定不是
        if (!hexStr.startsWith("55AAFF") && (!hexStr.startsWith("D5AAFF"))) return false // 如果不是55AAFF开头的，就不是协议数据
        // 这里一定注意
        if (byteArray.last() != crc8Maxim(
                byteArray, byteArray.size - 1
            ).toByte()
        ) return false  // 如果校验和不对，那么也不是协议数据

        return true
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
                sendData(txCmd.toByteArray())
                sendData(it)
            }
        }
    }


    //添加日志
    private fun addText(content: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            val textView = mBinding.tvLog
            textView.append(content)
            textView.append("\n")
            var offset = textView.lineCount * textView.lineHeight
            if (offset > textView.height) {
                textView.scrollTo(0, offset - textView.height + textView.lineHeight * 2)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serialHelper.onDestory()

    }
}

