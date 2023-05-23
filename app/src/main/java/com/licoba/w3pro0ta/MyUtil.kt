package com.licoba.w3pro0ta

import android.content.Context
import android.util.Log
import java.io.InputStream

object FileUtil {
    /**
     * 一个 Context 对象，
     * assets 文件夹中要读取的文件名，
     * 要读取的起始下标 startOffset
     * 要读取的数据长度 length
     */
    fun readBytesFromAsset(context: Context, fileName: String, start: Int, length: Int): ByteArray {
        val inputStream = context.assets.open(fileName)
        inputStream.skip(start.toLong())
        val buffer = ByteArray(length)
        inputStream.read(buffer)
        inputStream.close()
        return buffer
    }

    /**
     * 求和函数
     */
    fun getCheckSum(buf: ByteArray, len: Int): UInt {
        var crcSum: UInt = 0u
        for (i in 0 until len) {
            crcSum += buf[i].toUByte().toUInt()
        }
        return crcSum
    }

    fun toHexStringShort(value: UShort): String {
        return String.format("%04X", value)
    }

    fun toHexStringByte(value: UByte): String {
        return String.format("%02X", value)
    }



    fun toHexStringInt(value: UInt): String {
        return String.format("%08X", value)
    }


}