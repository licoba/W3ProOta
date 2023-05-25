package com.tmk.libserialhelper.tmk.upgrade

import android.content.Context

object UartUtil {
    /**
     * 一个 Context 对象，
     * assets 文件夹中要读取的文件名，
     * 要读取的起始下标 startOffset
     * 要读取的数据长度 length
     */
    fun readBytesFromAsset(
        context: Context,
        fileName: String,
        start: Int,
        length: Int
    ): ByteArray? {
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
     * 从字节数组byteArray的 指定位置 读取 指定长度  的数据
     */
    fun readByteArray(byteArray: ByteArray, start: Int, length: Int): ByteArray? {
        return try {
            // 创建一个新的字节数组，用于存储读取到的数据
            val result = ByteArray(length)
            // 将 byteArray 中从 start 位置开始、长度为 length 的数据复制到 result 数组中
            System.arraycopy(byteArray, start, result, 0, length)
            // 返回读取到的数据
            result
        } catch (e: Exception) {
            null
        }
    }


}