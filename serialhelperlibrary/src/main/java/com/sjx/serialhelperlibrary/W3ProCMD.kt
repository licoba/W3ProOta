package com.sjx.serialhelperlibrary

enum class W3ProCMD(val content: String,val hexContent:String) {
    // 客户端发送的指令
    START_UPD("START_UPD^_^","53544152545f5550445e5f5e"),

    // 收到的响应指令
    RECEIVE_START("RECEIVESTART","524543454956455354415254"),  // 蓝汛收到了我们发送的START_UPD指令
    //蓝汛向我们发送的读取数据指令，告诉我们可以发送数据给他了，通常是第一包数据，所以说addr是0
    WAIT_FIRST_PKG_DATA("WAIT_FIRST_PKG_DATA","AA550200000000000002000003010000") ,
    UPD_FINISH("UPD_FINISH","AA550301000000000000000003010000") , //蓝汛向我们发送升级完成的指令
}
