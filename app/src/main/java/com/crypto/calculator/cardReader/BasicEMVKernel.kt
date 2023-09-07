package com.crypto.calculator.cardReader

import android.nfc.tech.IsoDep
import android.util.Log
import com.crypto.calculator.extension.sendAPDU
import com.crypto.calculator.model.EMVTags
import com.crypto.calculator.model.getHexTag
import com.crypto.calculator.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.TlvUtil

abstract class BasicEMVKernel(nfcDelegate: NfcDelegate) : BasicNFCKernel(nfcDelegate) {
    private var cardData: HashMap<String, String> = hashMapOf()
    private var terminalData: HashMap<String, String> = hashMapOf()
    private var odaData = ""
    private val classTag = "BasicEMVKernel"

    override fun onStarted() {
        Log.d(classTag, "onStarted")
    }

    override fun onError(e: Exception) {
        super.onError(e)
        Log.e(classTag, "onError: $e")
        clearICCData()
        clearOdaData()
    }

    override fun onCompleted() {
        Log.d(classTag, "onCompleted")
        clearICCData()
        clearOdaData()
        clearTerminalData()
        super.onCompleted()
    }

    open fun communicator(isoDep: IsoDep, cmd: String): String {
        val cAPDU = cmd.uppercase()
        val rAPDU = isoDep.sendAPDU(cAPDU)
        if (rAPDU.endsWith(APDU_RESPONSE_CODE_OK)) {
            Log.i("communicator", "cAPDU ->>: $cAPDU")
            Log.i("communicator", "rAPDU <<-: $rAPDU")
        } else {
            Log.e("communicator", "cAPDU ->>: $cAPDU")
            Log.e("communicator", "rAPDU <<-: $rAPDU")
            throw Exception("Command not supported") // TODO: read sw1, sw2 to throw exception message
        }
        return rAPDU
    }

    fun processTlv(tlv: String) {
        val decodedMap = TlvUtil.parseTLV(tlv)
        Log.d("processTlv", "tag data: $decodedMap")
        val tmp = decodedMap.mapValues { it.value.first() }
        Log.d("processTlv", "tags to be save: $tmp")
        saveICCData(tmp)
    }

    fun saveICCData(data: Map<String, String>) {
        data.forEach {
            if (!TlvUtil.isTemplateTag(it.key) && !cardData.containsKey(it.key)) {
                cardData[it.key] = it.value
            }
        }
    }

    fun getICCTag(tag: String): String? {
        return cardData[tag]
    }

    private fun clearICCData() {
        cardData.clear()
    }

    fun getICCData() = cardData

    fun saveTerminalData(data: Map<String, String>) {
        terminalData += data
    }

    fun getTerminalTag(tag: String): String? {
        return terminalData[tag]
    }

    fun getTerminalData() = terminalData

    private fun clearTerminalData() {
        terminalData.clear()
    }

    fun saveOdaData(data: String) {
        odaData += data
    }

    fun getOdaData() = odaData

    private fun clearOdaData() {
        odaData = ""
    }

    open fun ppse(isoDep: IsoDep) {
        val tlv = communicator(isoDep, APDU_COMMAND_2PAY_SYS_DDF01)
        val appTemplates = TlvUtil.findByTag(tlv, tag = EMVTags.APPLICATION_TEMPLATE.getHexTag())
        Log.d("ppse", "appTemplates: $appTemplates")
        val finalTlv = appTemplates?.let { appList ->
            // check if more than 1 aid return
            if (appList.size > 1) {
                Log.d("ppse", "multiple AID read from card")
                // CTL -> auto select app with higher Application Priority Indicator
                appList.minBy { TlvUtil.decodeTLV(it)[EMVTags.APPLICATION_PRIORITY_INDICATOR.getHexTag()].toString().toInt(16) }
            } else {
                Log.d("ppse", "single AID read from card")
                appList.first()
            }
        }
        Log.d("ppse", "finalTlv: $finalTlv")
        finalTlv?.let {
            processTlv(it)
        }
    }
}