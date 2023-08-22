package com.crypto.calculator.service.cardSimulator.delegate

import android.util.Log
import com.crypto.calculator.service.cardSimulator.BasicEMVCardSimulator
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class AmexDelegate(private val iccData: HashMap<String, String>) : BasicEMVCardSimulator.EMVFlowDelegate {
    private val terminalData: HashMap<String, String> = hashMapOf()

    companion object {
        fun getInstance(iccData: HashMap<String, String>) = AmexDelegate(iccData)
        const val CVN01_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"
        const val CVN02_TAGS = "9F379F369F10"

        private fun readCVNFromIAD(iad: String): Int {
            try {
                val cvn = iad.substring(4, 6).toInt(16)
                Log.d("AmexDelegate", "readCVNFromIAD - cvn: $cvn")
                return cvn
            } catch (ex: Exception) {
                throw Exception("INVALID_ICC_DATA [9F10]")
            }
        }

        fun calculateAC(type: ApplicationCryptogram.Type, dolMap: HashMap<String, String>): String {
            val dataBuilder = StringBuilder()
            val cvn = dolMap["9F10"]?.let {
                readCVNFromIAD(it)
            } ?: 1
            val pan = dolMap["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
            val psn = dolMap["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
            val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")

            return when (type) {
                ApplicationCryptogram.Type.TC,
                ApplicationCryptogram.Type.ARQC -> {
                    when (cvn) {
                        1 -> {
                            TlvUtil.readTagList(CVN01_TAGS).forEach {
                                if (it != "9F10") {
                                    dataBuilder.append(dolMap[it])
                                } else {
                                    dataBuilder.append(dolMap[it]?.substring(6))
                                }
                            }
                            Encryption.calculateMAC(iccMK, dataBuilder.toString())?.uppercase() ?: throw Exception("CALCULATE_MAC_ERROR")
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
    }

    private fun calculateAC(type: ApplicationCryptogram.Type): String {
        val dataBuilder = StringBuilder()
        val cvn = iccData["9F10"]?.let {
            readCVNFromIAD(it)
        } ?: 1
        val pan = iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val psn = iccData["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
        val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")

        return when (type) {
            ApplicationCryptogram.Type.TC,
            ApplicationCryptogram.Type.ARQC -> {
                when (cvn) {
                    1 -> {
                        TlvUtil.readTagList(CVN01_TAGS).forEach {
                            if (it != "9F10") {
                                dataBuilder.append(terminalData[it] ?: iccData[it])
                            } else {
                                dataBuilder.append(iccData[it]?.substring(6))
                            }
                        }
                        Encryption.calculateMAC(iccMK, dataBuilder.toString())?.uppercase() ?: throw Exception("CALCULATE_MAC_ERROR")
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
        val data = cAPDU.substring(10).dropLast(2)
        val pdolMap = iccData["8C"]?.let { TlvUtil.readDOL(it) } ?: throw Exception("INVALID_ICC_DATA [8C]")
        var cursor = 0
        pdolMap.forEach {
            terminalData[it.key] = data.substring(cursor, cursor + it.value.toInt(16) * 2)
            cursor += it.value.toInt(16) * 2
        }
        Log.d("AmexDelegate", "processTerminalData - terminalData: $terminalData")
    }

    override fun onPPSEReply(cAPDU: String): String {
        Log.d("AmexDelegate", "onPPSEReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "325041592E5359532E4444463031",
                    "A5" to mapOf(
                        "BF0C" to mapOf(
                            "61" to mapOf(
                                "4F" to "A000000025010403",
                                "50" to "414D45524943414E2045585052455353",
                                "87" to "01",
                            )
                        )
                    )

                )
            )
        )
        Log.d("AmexDelegate", "onPPSEReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onSelectAIDReply(cAPDU: String): String {
        Log.d("AmexDelegate", "onSelectAIDReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "A000000025010403",
                    "A5" to mapOf(
                        "50" to "414D45524943414E2045585052455353",
                        "87" to "01",
                        "9F38" to iccData["9F38"],
                    )
                )
            )
        )
        Log.d("AmexDelegate", "onSelectAIDReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onExecuteGPOReply(cAPDU: String): String {
        Log.d("AmexDelegate", "onExecuteGPOReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "80" to "${iccData["82"]}08010100" // hardcoded AFL Tag[94] //0801010008060601080808001002020010050500
            )
        )
        Log.d("AmexDelegate", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onReadRecordReply(cAPDU: String): String {
        Log.d("AmexDelegate", "onReadRecordReply - cAPDU: $cAPDU")
        val rAPDU = when (cAPDU) {

            "00B2010C00" -> TlvUtil.encodeTLV(
                mapOf(
                    "70" to mapOf(
                        "57" to iccData["57"],
                        "5A" to iccData["5A"],
                        "5F24" to iccData["5F24"],
                        "5F34" to iccData["5F34"],
                        "8C" to iccData["8C"],
                        "8E" to iccData["8E"],
                        // mandatory for SPECTRA kernel otherwise ERROR: missing mandatory data -> abort  = ="
                        "5F20" to "585020434152442030382F56455220322E30",
                    )
                )
            )

            else -> "6A82"
        }
        Log.d("AmexDelegate", "onReadRecordReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onGenerateACReply(cAPDU: String): String {
        Log.d("AmexDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        processTerminalData(cAPDU)

        val sb = StringBuilder()
        sb.append(ApplicationCryptogram.getCryptogramInformationData(ApplicationCryptogram.Type.ARQC))
        sb.append(iccData["9F36"])
        sb.append(calculateAC(ApplicationCryptogram.Type.ARQC))
        sb.append(iccData["9F10"])
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "80" to sb.toString()
            )
        )
        Log.d("AmexDelegate", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onGetChallengeReply(cAPDU: String): String {
        Log.d("AmexDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        return "${UUidUtil.genHexIdByLength(16)}9000"
    }
}