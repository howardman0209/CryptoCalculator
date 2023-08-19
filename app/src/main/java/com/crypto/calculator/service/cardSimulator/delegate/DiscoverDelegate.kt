package com.crypto.calculator.service.cardSimulator.delegate

import android.util.Log
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.service.cardSimulator.BasicEMVCardSimulator
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class DiscoverDelegate(private val iccData: HashMap<String, String>) : BasicEMVCardSimulator.EMVFlowDelegate {
    private val terminalData: HashMap<String, String> = hashMapOf()

    companion object {
        fun getInstance(iccData: HashMap<String, String>) = DiscoverDelegate(iccData)
        const val CVN15_TAGS = "9F029F1A9F379F369F10"
        const val CVN16_TAGS = ""
    }

    private fun readCVNFromIAD(iad: String): Int {
        Log.d("DiscoverDelegate", "readCVNFromIAD - iad: $iad")
        val cvn = iad.substring(2, 4).toInt()
        Log.d("DiscoverDelegate", "readCVNFromIAD - cvn: $cvn")
        return cvn
    }

    private fun calculateAC(type: ApplicationCryptogram.Type): String {
        val dataBuilder = StringBuilder()
        val cvn = iccData["9F10"]?.let {
            readCVNFromIAD(it)
        } ?: 1
        val pan = iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val psn = iccData["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
//        val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")

        return when (type) {
            ApplicationCryptogram.Type.TC,
            ApplicationCryptogram.Type.ARQC -> {
                when (cvn) {
                    15 -> {
                        val atc = iccData["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
                        val sk = EMVUtils.deriveACSessionKey(pan, psn, atc) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
                        TlvUtil.readTagList(CVN15_TAGS).forEach {
                            dataBuilder.append(terminalData[it] ?: iccData[it])
                        }
                        Log.d("calculateAC", "DOL: $dataBuilder, SK: $sk")
                        Encryption.calculateMAC(sk, dataBuilder.toString().applyPadding(PaddingMethod.ISO9797_1_M2)).uppercase()
                    }

                    else -> {
                        // TODO: calculate other CVN
                        throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                    }
                }
            }

            ApplicationCryptogram.Type.AAC -> {
                // TODO: calculate AAC
                ""
            }
        }
    }

    private fun processTerminalData(cAPDU: String) {
        val data = cAPDU.substring(14).dropLast(2)
        val pdolMap = iccData["9F38"]?.let { TlvUtil.readDOL(it) } ?: throw Exception("INVALID_ICC_DATA [9F38]")
        var cursor = 0
        pdolMap.forEach {
            terminalData[it.key] = data.substring(cursor, cursor + it.value.toInt(16) * 2)
            cursor += it.value.toInt(16) * 2
        }
        Log.d("DiscoverDelegate", "processTerminalData - terminalData: $terminalData")
    }

    override fun onPPSEReply(cAPDU: String): String {
        Log.d("DiscoverDelegate", "onPPSEReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "325041592E5359532E4444463031",
                    "A5" to mapOf(
                        "BF0C" to mapOf(
                            "61" to mapOf(
                                "50" to "446973636F766572",
                                "87" to "01",
                                "4F" to "A0000001523010",
                                "9F2A" to "0006",
                            )
                        )
                    )

                )
            )
        )
        Log.d("DiscoverDelegate", "onPPSEReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onSelectAIDReply(cAPDU: String): String {
        Log.d("DiscoverDelegate", "onSelectAIDReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "A0000001523010",
                    "A5" to mapOf(
                        "50" to "446973636F766572",
                        "87" to "01",
                        "9F11" to "01",
                        "9F38" to iccData["9F38"],
                    )
                )
            )
        )
        Log.d("DiscoverDelegate", "onSelectAIDReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onExecuteGPOReply(cAPDU: String): String {
        Log.d("DiscoverDelegate", "onExecuteGPOReply - cAPDU: $cAPDU")

        processTerminalData(cAPDU)

        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "77" to mapOf(
                    "57" to iccData["57"],
                    "82" to iccData["82"],
                    "5F25" to iccData["5F25"],
                    "5F28" to iccData["5F28"],
                    "5F34" to iccData["5F34"],
                    "9F08" to  iccData["9F08"],
                    "9F10" to iccData["9F10"],
                    "9F26" to calculateAC(ApplicationCryptogram.Type.ARQC),
                    "9F27" to ApplicationCryptogram.getCryptogramInformationData(ApplicationCryptogram.Type.ARQC),
                    "9F36" to iccData["9F36"],
                    "9F71" to iccData["9F71"],
                )
            )
        )
        Log.d("DiscoverDelegate", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onReadRecordReply(cAPDU: String): String {
        Log.d("DiscoverDelegate", "onReadRecordReply - cAPDU: $cAPDU")
        return ""
    }

    override fun onGenerateACReply(cAPDU: String): String {
        Log.d("DiscoverDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        return ""
    }

    override fun onGetChallengeReply(cAPDU: String): String {
        Log.d("DiscoverDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        return "${UUidUtil.genHexIdByLength(16)}9000"
    }
}