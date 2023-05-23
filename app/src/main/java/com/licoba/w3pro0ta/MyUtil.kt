package com.licoba.w3pro0ta

import android.content.Context

object MyUtil {
    /**
     * 一个 Context 对象，
     * assets 文件夹中要读取的文件名，
     * 要读取的起始下标 startOffset
     * 要读取的数据长度 length
     */
    fun readBytesFromAsset(context: Context, fileName: String, start: Int, length: Int): ByteArray? {
        return try {
            val inputStream = context.assets.open(fileName)
            inputStream.skip(start.toLong())
            val buffer = ByteArray(length)
            inputStream.read(buffer)
            inputStream.close()
            buffer
        } catch (e: Exception) {
            null
        }

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



}