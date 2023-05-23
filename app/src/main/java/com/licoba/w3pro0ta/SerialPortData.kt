package com.licoba.w3pro0ta

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

class SerialPortData {
}

// 收到的数据的结构体，对应C代码中的 `uart_upd_m_rxcmd_t`
// 用 UShort、UByte 和 UInt 来分别代表 C 中的 u16、u8 和 u32
data class UartUpdMRxCmd(
    var sign: UShort = 0u,
    var cmd: UByte = 0u,
    var status: UByte = 0u,
    var addr: UInt = 0u,
    var len: UInt = 0u,
    var crc: UShort = 0u,
    var end: UShort = 0u // 用来补足的
) {
    fun parseSelf(bytes: ByteArray) { // 解析数据到self，赋值给自己的成员变量
//        val data = ByteBuffer.wrap(bytes) // Java默认是大端吗，这里换成小端解析
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN) // Java默认是大端吗，这里换成小端解析
        sign = data.short.toUShort()
        cmd = data.get().toUByte()
        status = data.get().toUByte()
        addr = data.int.toUInt()
        len = data.int.toUInt()
        crc = data.short.toUShort()
        end = data.short.toUShort()
    }


    // 将每个属性叠加，转化为字节数组
    fun toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putShort(sign.toShort())
        byteBuffer.put(cmd.toByte())
        byteBuffer.put(status.toByte())
        byteBuffer.putInt(addr.toInt())
        byteBuffer.putInt(len.toInt())
        byteBuffer.putShort(crc.toShort())
        byteBuffer.putShort(end.toShort())
        return byteBuffer.array()
    }
}

/**
 *
typedef struct
{
u16 sign;   2
u8 cmd;     1
u8 status;  1
u32 addr;   4
u32 len;    4
u16 crc;    2
}uart_upd_m_rxcmd_t;
 */


data class UartUpdMTxCmd(
    var sign: UShort = 0u,
    var cmd: UByte = 0u,
    var status: UByte = 0u,
    var addr: UInt = 0u,
    var data_crc: UInt = 0u,
    var crc: UShort = 0u,
) {
    fun parseSelf(bytes: ByteArray) { // 解析数据到self，赋值给自己的成员变量
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
//        val data = ByteBuffer.wrap(bytes)
        sign = data.short.toUShort()
        cmd = data.get().toUByte()
        status = data.get().toUByte()
        addr = data.int.toUInt()
        data_crc = data.int.toUInt()
        crc = data.short.toUShort()
    }


    // 将每个属性叠加，转化为字节数组
    fun toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN) //表示字节序为小端。
        byteBuffer.putShort(sign.toShort())
        byteBuffer.put(cmd.toByte())
        byteBuffer.put(status.toByte())
        byteBuffer.putInt(addr.toInt())
        byteBuffer.putInt(data_crc.toInt())
        byteBuffer.putShort(crc.toShort())
        return byteBuffer.array()
    }



    fun printString(): String {
        val hexString = buildString {
            append(" sign:")
            append(sign.toString(16).padStart(4, '0'))
            append(" cmd:")
            append(cmd.toString(16).padStart(2, '0'))
            append(" status:")
            append(status.toString(16).padStart(2, '0'))
            append(" addr:")
            append(addr.toString(16).padStart(8, '0'))
            append(" data_crc:")
            append(data_crc.toString(16).padStart(8, '0'))
            append(" crc:")
            append(crc.toString(16).padStart(4, '0'))
        }

        return hexString.uppercase()
    }


}


