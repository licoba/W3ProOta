package com.tmk.libserialhelper.tmk.upgrade

import SerialUtil.getCheckSum
import android.content.Context
import android.util.Log
import com.tmk.libserialhelper.DataConversion
import com.tmk.libserialhelper.OnUsbDataListener
import com.tmk.libserialhelper.SerialHelper
import com.tmk.libserialhelper.tmk.UartUpdMTxCmd
import com.tmk.libserialhelper.tmk.W3ProUpgradeCMD
import com.tmk.libserialhelper.tmk.upgrade.UartUtil.readByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UartOtaManager(context: Context, private val serialHelper: SerialHelper) {

    private var reqUpgradeJob: Job? = null

    var config: UartConfig = UartConfig()
        set(value) {
            field = value
            logD("更新配置：$value")
        }

    var listener: UartEventListener? = null

    // USB数据监听
    private val mUsbDataListener = object : OnUsbDataListener {
        override fun onDataError(e: Exception?) {
            logE("数据异常 $e")
        }

        override fun onDataReceived(bytes: ByteArray) {
            // 处理返回的数据, 当前线程为子线程
            correctionByteData(bytes)
            processReceivedData(bytes)
        }
    }

    init {
        serialHelper.addOnUsbDataListener(mUsbDataListener)
    }

    companion object {
        val TAG: String = UartOtaManager::class.java.simpleName
        fun getInstance(context: Context, serialHelper: SerialHelper): UartOtaManager {
            return UartOtaManager(context, serialHelper)
        }
    }


    private fun requestReadyOta() {
        logD("发送请求升级命令")
        listener?.onOtaPrepare()
        reqUpgradeJob = CoroutineScope(Dispatchers.IO).launch {
            repeat(20) {
                delay(200)
                sendData(DataConversion.decodeHexString(W3ProUpgradeCMD.START_UPD.hexContent))
            }
            logE(UartError.CAN_NOT_RECEIVE_START_CMD.errDesc)
            listener?.onOtaError(UartError.CAN_NOT_RECEIVE_START_CMD)
        }
    }

    private fun sendData(bytes: ByteArray) {
        logD("发->${DataConversion.encodeHexString(bytes)}")
        serialHelper.write(bytes)
    }


    fun startOTA() {
        logD("开始升级")
        requestReadyOta()
    }


    interface UartEventListener {
        fun onOtaPrepare()  // OTA 升级正在准备，等待蓝汛回复
        fun onOtaStart()  //OTA 升级已经准备好， 开始OTA升级
        fun onOtaProgress(progress: Float) // OTA升级进度
        fun onOtaStop()
        fun onOtaFinish()
        fun onOtaPause()  //  升级完成
        fun onOtaContinue()
        fun onOtaError(err: UartError)
    }

    private fun logD(str: String) {
        if (!config.openLog) return
        Log.d(TAG, str)
    }

    private fun logE(str: String) {
        if (!config.openLog) return
        Log.e(TAG, str)
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


    /***
     * 处理收到的数据（只有数据合法才处理）
     */
    private fun processReceivedData(bytes: ByteArray) {
        // 注意：contentToString是十六进制数据
        logD("收到了数据  ${DataConversion.encodeHexString(bytes)}")
        if (bytes.decodeToString() == W3ProUpgradeCMD.RECEIVE_START.content) {
            reqUpgradeJob?.cancel()
            listener?.onOtaStart()
            logD("蓝汛已收到开始UPD升级指令...")
        } else if (isWaitingDataPkg(bytes)) {
            logD("蓝汛等待发送升级包数据...")
            sendUpdData(bytes)
        } else if (isCheckUartPkg(bytes)) {  // 直接原封不动返回这个包即可
            logD("蓝汛等待回复确认为upd模式...")
            sendData(bytes)
        } else if (isUpdSuccessPkg(bytes)) {
            listener?.onOtaFinish()
            logD("😁升级完成！！！")
        } else if (isUpdFailPkg(bytes)) {
            listener?.onOtaError(UartError.REFUSED_BY_DEVICE_OTA_FILE_ERROR)
            logD("😡升级失败！！！")
        }
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
     * 判断是不是等待我们发送数据的包，
     * 有一个特征就是以SIGN:55AA CMD:02 开头
     */
    private fun isWaitingDataPkg(bytes: ByteArray): Boolean {
        return DataConversion.encodeHexString(bytes).uppercase().startsWith("AA5502")
    }


    /**
     * 是否是检查Upd模式的包，如果是，就按照文档，直接返回就行了
     */
    private fun isCheckUartPkg(bytes: ByteArray): Boolean {
        return DataConversion.encodeHexString(bytes).uppercase().startsWith("AA5501")
    }


    /**
     * 是否是升级完成的包，如果是，那么提示用户就行了
     */
    private fun isUpdSuccessPkg(bytes: ByteArray): Boolean {
        return DataConversion.encodeHexString(bytes).uppercase().startsWith("AA5503FF")
    }


    /**
     * 是升级失败的包
     */
    private fun isUpdFailPkg(bytes: ByteArray): Boolean {
        // 只有AA5503FF才是升级成功，AA550306就是升级失败了
        val str = DataConversion.encodeHexString(bytes).uppercase()
        return str.startsWith("AA5503") && !str.startsWith("AA5503FF")
    }


    private fun sendUpdData(bytes: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5)    // 去掉延迟是1分46秒
//            val rxCmd = UartUpdMRxCmd().apply { parseSelf(bytes) }  // 接收的指令包
            val txCmd = UartUpdMTxCmd().apply { parseSelf(bytes) }  // 发送的指令包
            // 获取512字节的文件
            logD("解析后的txCmd: ${txCmd.printString()}")
            val start = txCmd.addr.toInt()
            val percent = start.toFloat() / config.otaData.size
            listener?.onOtaProgress(percent)
            val byteArrayFile = readByteArray(config.otaData, txCmd.addr.toInt(), 512)
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


}