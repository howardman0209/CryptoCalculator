package com.crypto.calculator.cardReader

import android.nfc.tech.IsoDep
import android.util.Log
import com.crypto.calculator.extension.sendAPDU
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


    abstract fun ppse(isoDep: IsoDep)

    abstract fun selectAID(isoDep: IsoDep)

    abstract fun executeGPO(isoDep: IsoDep)

    abstract fun readRecord(isoDep: IsoDep)

    abstract fun generateAC(isoDep: IsoDep)

    abstract fun getChallenge(isoDep: IsoDep): String?

    abstract fun performODA()

    abstract fun performCVM()
}