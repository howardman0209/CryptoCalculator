package com.crypto.calculator.cardReader.contactless

import android.util.Log
import com.crypto.calculator.cardReader.EmvKernel
import com.crypto.calculator.cardReader.contactless.delegate.EMVCTLProcess

abstract class BasicCTLKernel(private val core: EmvKernel) : EMVCTLProcess {
    val context = core.context

    fun communicator(cAPDU: String): String {
        return core.communicator(cAPDU)
    }

    fun processTLV(rAPDU: String) {
        core.processTlv(rAPDU)
    }

    open fun onTapEmvProcess() {
        Log.d("BaseCTLKernel", "onTapEmvProcess start")
    }

    open fun postTapEmvProcess() {
        Log.d("BaseCTLKernel", "postTapEmvProcess start")
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