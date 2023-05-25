package com.licoba.w3pro0ta

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tmk.libserialhelper.tmk.upgrade.UartConfig
import com.tmk.libserialhelper.tmk.upgrade.UartOtaManager

class TestOtaActivity : AppCompatActivity() {

    lateinit var context: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_ota)
        context = this@TestOtaActivity

    }
}