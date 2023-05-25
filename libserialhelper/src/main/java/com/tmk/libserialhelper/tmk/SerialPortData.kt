package com.tmk.libserialhelper.tmk

import toHex
import java.nio.ByteBuffer
import java.nio.ByteOrder

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


/**
 * W3Pro蓝汛串口通信的每包完整数据
 * 比如 55AAFF EF 10 FF02005C100A0010CEA02A15292F070182
 * 总长22
 */
data class W3TotalPacket(
    var head: UByteArray = UByteArray(3),
    var cmd: Byte = 0,
    var dataLength: Byte = 0,
    var data: W3PacketData,
    var check: Byte = 0,
) {
    override fun toString() = buildString {
        append("W3TotalPacket(")
        append("head=[${head.joinToString(separator = ",") { it.toHex().uppercase() }}], ")
        append("cmd=${cmd.toHex().uppercase()}, ")
        append("dataLength=${dataLength.toHex().uppercase()}, ")
        append("data=${data}, ")
        append("check=${check.toHex().uppercase()}")
        append(")")
    }
}

/**
 * W3每包完整数据中的data字段
 * 例如：FF 02 02 57 0F C8 00 20 E3 3A 27 CB 11 78 07 01
 * 总长 16
 */
data class W3PacketData(
    var result: Byte = 0, //结果 |  0:异常 1:异常 2:完成
    var mode: Byte = 0, // 模式 | 0: 未设置  1:厂测模式  2:用户模式
    var headsetRole: Byte = 0, //耳机角色 | 0: 未设置  1:左耳  2:右耳
    var electric: Byte = 0, // 电量 | 单位%  5A 代表电量80
    var voltage: Short = 0,// 电压 | 一般不用  0FF1  转成10进制是  4081  代表电压4081mv
    var lanXunFirmV: Short = 0, // 蓝讯固件版本 | 每个字符范围为0-9，第二个字节只用到一半，高4位   0010  代表0.0.1  最后一位没用
    var bleMac: UByteArray = UByteArray(6), // BLE Mac地址 | CE,A0,2A,15,29,2F  用冒号拼接就行了，不用转换进制
    var bleFirmV: Short = 0, //BLE固件版本 | 前两个字符范围为0-9，用1个字节表示，第二个字节表示最后一个字符，范围为1-99  0701  代表0.7.1   070a代表版本号 0.7.10

) {
    override fun toString() = buildString {
        append("W3PacketData(")
        append("result=${result.toHex().uppercase()}, ")
        append("mode=${mode.toHex().uppercase()}, ")
        append("headsetRole=${headsetRole.toHex().uppercase()}, ")
        append("electric=${electric.toHex().uppercase()}, ")
        append("voltage=${voltage.toHex().uppercase()}, ")
        append("lanXunFirmV=${lanXunFirmV.toHex().uppercase()}, ")
        append("bleMac=[${bleMac.joinToString(separator = ",") { it.toHex().uppercase() }}], ")
        append("bleFirmV=${bleFirmV.toHex().uppercase()}")
        append(")")
    }
}


/**
 * W3发送的数据包字段
 * 例如：FF 02 02 57 0F C8 00 20 E3 3A 27 CB 11 78 07 01
 * 总长 16
 */
data class W3SendPacket(
    var head: UByteArray = ubyteArrayOf(0x55u, 0xaaU, 0xffu),
    var cmd: Byte = 0,
    var dataLength: Byte = 0,
    var data: UByteArray,
    var check: Byte = 0,
) {
    override fun toString() = buildString {
        append("W3SendPacket(")
        append("head=[${head.joinToString(separator = ",") { it.toHex().uppercase() }}], ")
        append("cmd=${cmd.toHex().uppercase()}, ")
        append("dataLength=${dataLength.toHex().uppercase()}, ")
        append("data=${data}, ")
        append("check=${check.toHex().uppercase()}")
        append(")")
    }


    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(6 + data.size)
        buffer.put(head.toByteArray())
        buffer.put(cmd)
        buffer.put(dataLength)
        buffer.put(data.toByteArray())
        buffer.put(check)
        return buffer.array()
    }

}



