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
    var result: Byte = 0, // 0:异常 1:异常 2:完成
    var mode: Byte = 0, //0: 未设置  1:厂测模式  2:用户模式
    var headsetRole: Byte = 0, // 0: 未设置  1:左耳  2:右耳
    var electric: Byte = 0, // 电量，单位%  5A 代表电量80
    var voltage: Short = 0,// 电压，一般不用  0FF1  转成10进制是  4081  代表电压4081mv
    var lanXunFirmV: Short = 0, // 蓝讯固件版本，每个字符范围为0-9，第二个字节只用到一半，高4位   0010  代表0.0.1  最后一位没用
    var bleMac: UByteArray = UByteArray(6), //BLE Mac地址  CE,A0,2A,15,29,2F  用冒号拼接
    var bleFirmV: Short = 0, //BLE 固件版本，前两个字符范围为0-9，用1个字节表示，第二个字节表示最后一个字符，范围为1-99  0701  代表0.7.1   070a代表版本号 0.7.10

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


fun Byte.toHex(): String = "%02x".format(this)
fun Short.toHex(): String = "%04x".format(this)
fun UByte.toHex(): String = "%02x".format(toInt())


// 计算sum的函数
fun crc8Maxim(buf: ByteArray, length: Int): UByte {
    var crc = 0.toUByte()
    for (i in 0 until length) {
        crc = vusbCrc8Tbl[(crc.toInt() xor buf[i].toInt()).and(0xFF)]
    }
    return crc
}


val vusbCrc8Tbl = ubyteArrayOf(
    0x00u, 0x5eu, 0xbcu, 0xe2u, 0x61u, 0x3fu, 0xddu, 0x83u,
    0xc2u, 0x9cu, 0x7eu, 0x20u, 0xa3u, 0xfdu, 0x1fu, 0x41u,
    0x9du, 0xc3u, 0x21u, 0x7fu, 0xfcu, 0xa2u, 0x40u, 0x1eu,
    0x5fu, 0x01u, 0xe3u, 0xbdu, 0x3eu, 0x60u, 0x82u, 0xdcu,
    0x23u, 0x7du, 0x9fu, 0xc1u, 0x42u, 0x1cu, 0xfeu, 0xa0u,
    0xe1u, 0xbfu, 0x5du, 0x03u, 0x80u, 0xdeu, 0x3cu, 0x62u,
    0xbeu, 0xe0u, 0x02u, 0x5cu, 0xdfu, 0x81u, 0x63u, 0x3du,
    0x7cu, 0x22u, 0xc0u, 0x9eu, 0x1du, 0x43u, 0xa1u, 0xffu,
    0x46u, 0x18u, 0xfau, 0xa4u, 0x27u, 0x79u, 0x9bu, 0xc5u,
    0x84u, 0xdau, 0x38u, 0x66u, 0xe5u, 0xbbu, 0x59u, 0x07u,
    0xdbu, 0x85u, 0x67u, 0x39u, 0xbau, 0xe4u, 0x06u, 0x58u,
    0x19u, 0x47u, 0xa5u, 0xfbu, 0x78u, 0x26u, 0xc4u, 0x9au,
    0x65u, 0x3bu, 0xd9u, 0x87u, 0x04u, 0x5au, 0xb8u, 0xe6u,
    0xa7u, 0xf9u, 0x1bu, 0x45u, 0xc6u, 0x98u, 0x7au, 0x24u,
    0xf8u, 0xa6u, 0x44u, 0x1au, 0x99u, 0xc7u, 0x25u, 0x7bu,
    0x3au, 0x64u, 0x86u, 0xd8u, 0x5bu, 0x05u, 0xe7u, 0xb9u,
    0x8cu, 0xd2u, 0x30u, 0x6eu, 0xedu, 0xb3u, 0x51u, 0x0fu,
    0x4eu, 0x10u, 0xf2u, 0xacu, 0x2fu, 0x71u, 0x93u, 0xcdu,
    0x11u, 0x4fu, 0xadu, 0xf3u, 0x70u, 0x2eu, 0xccu, 0x92u,
    0xd3u, 0x8du, 0x6fu, 0x31u, 0xb2u, 0xecu, 0x0eu, 0x50u,
    0xafu, 0xf1u, 0x13u, 0x4du, 0xceu, 0x90u, 0x72u, 0x2cu,
    0x6du, 0x33u, 0xd1u, 0x8fu, 0x0cu, 0x52u, 0xb0u, 0xeeu,
    0x32u, 0x6cu, 0x8eu, 0xd0u, 0x53u, 0x0du, 0xefu, 0xb1u,
    0xf0u, 0xaeu, 0x4cu, 0x12u, 0x91u, 0xcfu, 0x2du, 0x73u,
    0xcau, 0x94u, 0x76u, 0x28u, 0xabu, 0xf5u, 0x17u, 0x49u,
    0x08u, 0x56u, 0xb4u, 0xeau, 0x69u, 0x37u, 0xd5u, 0x8bu,
    0x57u, 0x09u, 0xebu, 0xb5u, 0x36u, 0x68u, 0x8au, 0xd4u,
    0x95u, 0xcbu, 0x29u, 0x77u, 0xf4u, 0xaau, 0x48u, 0x16u,
    0xe9u, 0xb7u, 0x55u, 0x0bu, 0x88u, 0xd6u, 0x34u, 0x6au,
    0x2bu, 0x75u, 0x97u, 0xc9u, 0x4au, 0x14u, 0xf6u, 0xa8u,
    0x74u, 0x2au, 0xc8u, 0x96u, 0x15u, 0x4bu, 0xa9u, 0xf7u,
    0xb6u, 0xe8u, 0x0au, 0x54u, 0xd7u, 0x89u, 0x6bu, 0x35u
)