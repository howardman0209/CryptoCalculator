package com.crypto.calculator.cardReader.contactless

import android.nfc.tech.IsoDep
import android.util.Log
import com.crypto.calculator.cardReader.EMVCore
import com.crypto.calculator.cardReader.contactless.delegate.EMVCTLProcess

abstract class BasicCTLKernel(private val core: EMVCore): EMVCTLProcess {
    val context = core.context

    fun communicator(isoDep: IsoDep, cAPDU: String): String {
        return core.communicator(isoDep, cAPDU)
    }

    fun processTLV(rAPDU: String) {
        core.processTlv(rAPDU)
    }

    open fun emvProcess(isoDep: IsoDep) {
        Log.d("BaseCTLKernel", "emvProcess start")
    }

    fun saveICCData(data: Map<String, String>) {
        core.saveICCData(data)
    }

    fun getICCTag(tag: String): String? {
        return core.getICCTag(tag)
    }

    fun saveTerminalData(data: Map<String, String>) {
        core.saveTerminalData(data)
    }

    fun getTerminalTag(tag: String): String? {
        return core.getTerminalTag(tag)
    }

    fun saveOdaData(data: String) {
        core.saveOdaData(data)
    }

    fun getOdaData() = core.getOdaData()
}