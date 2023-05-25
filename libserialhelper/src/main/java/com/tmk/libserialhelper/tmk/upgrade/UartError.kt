package com.tmk.libserialhelper.tmk.upgrade

enum class UartError(errCode: Int, val errDesc: String) {
    CAN_NOT_RECEIVE_START_CMD(
        1008,
        "蓝汛无响应！超时未收到 RECEIVE_START 响应，请检查设备是否开机"
    ),
    REFUSED_BY_DEVICE_OTA_FILE_ERROR(2001, "设备拒绝了升级，升级文件格式有误！"),
    NOT_CONNECTED(1000, "设备未连接"),
    NOT_INIT(1001, "未初始化"),
    NOT_FOUND_OTA_SERVICE(1003, "找不到OTA服务（FF12）"),
    NOT_FOUND_OTA_DATA_IN(1004, "设备没有找到dataInCharacteristic（FF14）"),
    NOT_FOUND_OTA_DATA_OUT(1005, "设备没有找到dataOutCharacteristic（FF15）"),
    NOT_FOUND_OTA_CHARACTERISTIC(1006, "找不到dataInCharacteristic或者dataOutCharacteristic"),
    NOT_FOUND_CLIENT_CHARACTERISTIC_CONFIG(1007, "获取不到Client Characteristic config"),
    NOT_FOUND_NON_PRIMARY_DEVICE(1009, "扫描副耳时发生错误"),
    TIMEOUT_SCAN_NON_PRIMARY_DEVICE(1010, "扫描副耳超时"),
    REPORT_FROM_DEVICE(2000, "有点问题，错误代码"),
    TIMEOUT_RECEIVE_RESPONSE(2002, "等待设备回复超时");
}