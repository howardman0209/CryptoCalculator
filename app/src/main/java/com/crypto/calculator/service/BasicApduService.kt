package com.crypto.calculator.service

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString

abstract class BasicApduService : HostApduService() {
    override fun onCreate() {
        super.onCreate()
        Log.d("BasicApduService", "onCreate")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BasicApduService", "onDestroy")
    }

    override fun processCommandApdu(p0: ByteArray?, p1: Bundle?): ByteArray {
        val cmd = commandReceiver(p0)
        Log.d("BasicApduService", "cmd: $cmd")
        val tlv = responseConstructor(cmd)
        Log.d("BasicApduService", "tlv: $tlv")
        return tlv.hexToByteArray()
    }

    override fun onDeactivated(p0: Int) {
        Log.d("BasicApduService", "onDeactivated p0:$p0")
    }

    private fun commandReceiver(cAPDU: ByteArray?): String? {
        return cAPDU?.toHexString()?.uppercase()
    }

    abstract fun responseConstructor(cAPDU: String?): String
}