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

    fun communicator(isoDep: IsoDep, cmd: String): String {
        val command = cmd.uppercase()
        val tlv = isoDep.sendAPDU(command)
        if (tlv.endsWith(APDU_RESPONSE_CODE_OK)) {
            Log.i("APDU ->>", "cmd: $command")
            Log.i("APDU <<-", "tlv: $tlv")
        } else {
            Log.e("APDU ->>", "cmd: $command")
            Log.e("APDU <<-", "tlv: $tlv")
        }
        return tlv
    }

    fun processTlv(tlv: String) {
        val decodedMap = TlvUtil.decodeTLV(tlv)
        Log.d("processTlv", "tag data: $decodedMap")
        saveICCData(decodedMap)
    }

    private fun saveICCData(data: Map<String, Any?>) {
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