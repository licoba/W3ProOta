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
    fun readBytesFromAsset(context: Context, fileName: String, startOffset: Long, length: Int): ByteArray? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(fileName)
            val totalSize = inputStream.available().toLong()
            if (startOffset >= totalSize) {
                return null
            }
            inputStream.skip(startOffset)
            val byteArray = ByteArray(length)
            inputStream.read(byteArray, 0, length.coerceAtMost((totalSize - startOffset).toInt()))
            return byteArray
        } catch (ex: Exception) {
            Log.e("TAG", "Error reading file $fileName from assets: ${ex.message}")
        } finally {
            inputStream?.close()
        }
        return null
    }


}