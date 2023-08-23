package com.crypto.calculator.service.cardSimulator.delegate

import android.util.Log
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.service.cardSimulator.BasicEMVCard
import com.crypto.calculator.service.cardSimulator.BasicEMVService
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class UnionPayDelegate(private val iccData: HashMap<String, String>): BasicEMVCard(iccData), BasicEMVService.EMVFlowDelegate {
    private val terminalData: HashMap<String, String> = hashMapOf()

    companion object {
        fun getInstance(iccData: HashMap<String, String>) = UnionPayDelegate(iccData)
        const val CVN01_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"

        fun readCVNFromIAD(iad: String): Int {
            try {
                Log.d("UnionPaySimulator", "readCVNFromIAD - iad: $iad")
                val cvn = iad.substring(4, 6).toInt(16)
                Log.d("UnionPaySimulator", "readCVNFromIAD - cvn: $cvn")
                return cvn
            } catch (ex: Exception) {
                throw Exception("INVALID_ICC_DATA [9F10]")
            }
        }

        fun getAcDOL(data: HashMap<String, String>, cvn: Int? = 1): String {
            val dolBuilder = StringBuilder()
            return when (cvn) {
                1-> {
                    TlvUtil.readTagList(CVN01_TAGS).forEach {
                        if (it != "9F10") {
                            dolBuilder.append(data[it])
                        } else {
                            dolBuilder.append(data[it]?.substring(6, 14))
                        }
                    }
                    dolBuilder.toString().uppercase()
                }

                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }

        fun getAcPaddingMethod(cvn: Int? = 1): PaddingMethod {
            return when (cvn) {
                1 -> PaddingMethod.ISO9797_1_M2
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }

        fun calculateAC(type: ApplicationCryptogram.Type, dolMap: HashMap<String, String>): String {
            val dataBuilder = StringBuilder()
            val cvn = dolMap["9F10"]?.let {
                readCVNFromIAD(it)
            } ?: 1
            val pan = dolMap["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
            val psn = dolMap["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
//        val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")

            return when (type) {
                ApplicationCryptogram.Type.TC,
                ApplicationCryptogram.Type.ARQC -> {
                    when (cvn) {
                        1 -> {
                            val atc = dolMap["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
                            val sk = EMVUtils.deriveACSessionKey(pan, psn, atc) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
                            TlvUtil.readTagList(CVN01_TAGS).forEach {
                                if (it != "9F10") {
                                    dataBuilder.append(dolMap[it])
                                } else {
                                    dataBuilder.append(dolMap[it]?.substring(6, 14))
                                }
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
                    1 -> {
                        val atc = iccData["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
                        val sk = EMVUtils.deriveACSessionKey(pan, psn, atc) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
                        TlvUtil.readTagList(CVN01_TAGS).forEach {
                            if (it != "9F10") {
                                dataBuilder.append(terminalData[it] ?: iccData[it])
                            } else {
                                dataBuilder.append(iccData[it]?.substring(6, 14))
                            }
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
        Log.d("UnionPaySimulator", "processTerminalData - terminalData: $terminalData")
    }

    override fun onPPSEReply(cAPDU: String): String {
        Log.d("UnionPaySimulator", "onPPSEReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "325041592E5359532E4444463031",
                    "A5" to mapOf(
                        "BF0C" to mapOf(
                            "61" to mapOf(
                                "4F" to "A000000333010102",
                                "50" to "5649534120435245444954"
                            )
                        )
                    )

                )
            )
        )
        Log.d("UnionPaySimulator", "onPPSEReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onSelectAIDReply(cAPDU: String): String {
        Log.d("UnionPaySimulator", "onSelectAIDReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "A000000333010102",
                    "A5" to mapOf(
                        "9F38" to iccData["9F38"],
                        "BF0C" to mapOf(
                            "9F5A" to iccData["9F5A"]
                        )
                    )
                )
            )
        )
        Log.d("UnionPaySimulator", "onSelectAIDReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onExecuteGPOReply(cAPDU: String): String {
        Log.d("UnionPaySimulator", "onExecuteGPOReply - cAPDU: $cAPDU")

        processTerminalData(cAPDU)

        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "77" to mapOf(
                    "57" to iccData["57"],
                    "82" to iccData["82"],
                    "5F34" to iccData["5F34"],
                    "9F10" to iccData["9F10"],
                    "9F26" to calculateAC(ApplicationCryptogram.Type.ARQC),
                    "9F27" to ApplicationCryptogram.getCryptogramInformationData(ApplicationCryptogram.Type.ARQC),
                    "9F36" to iccData["9F36"],
                    "9F6C" to iccData["9F6C"],
                    "9F6E" to iccData["9F6E"],
                )
            )
        )
        Log.d("UnionPaySimulator", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onReadRecordReply(cAPDU: String): String {
        Log.d("UnionPaySimulator", "onReadRecordReply - cAPDU: $cAPDU")
        return ""
    }

    override fun onGenerateACReply(cAPDU: String): String {
        Log.d("UnionPaySimulator", "onGenerateACReply - cAPDU: $cAPDU")
        return ""
    }

    override fun onGetChallengeReply(cAPDU: String): String {
        Log.d("UnionPaySimulator", "onGenerateACReply - cAPDU: $cAPDU")
        return "${UUidUtil.genHexIdByLength(16)}9000"
    }
}