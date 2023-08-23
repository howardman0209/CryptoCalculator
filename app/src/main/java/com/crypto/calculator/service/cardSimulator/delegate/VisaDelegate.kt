package com.crypto.calculator.service.cardSimulator.delegate

import android.util.Log
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.service.cardSimulator.BasicEMVCard
import com.crypto.calculator.service.cardSimulator.BasicEMVService
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.service.model.ApplicationCryptogram.getCryptogramInformationData
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class VisaDelegate(private val iccData: HashMap<String, String>) : BasicEMVCard(iccData), BasicEMVService.EMVFlowDelegate {
    companion object {
        fun getInstance(iccData: HashMap<String, String>) = VisaDelegate(iccData)
        const val CVN10_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"
        const val CVN17_TAGS = "9F029F379F369F10"
        const val CVN18_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"

        fun readCVNFromIAD(iad: String): Int {
            try {
                val cvn = when (iad.substring(6, 8)) {
                    "03" -> iad.substring(4, 6).toInt(16)
                    "00" -> iad.substring(2, 4).toInt(16)
                    else -> throw Exception("UNKNOWN_IAD_FORMAT")
                }
                Log.d("VisaSimulator", "readCVNFromIAD - cvn: $cvn")
                return cvn
            } catch (ex: Exception) {
                throw Exception("INVALID_ICC_DATA [9F10]")
            }
        }

        fun getAcDOL(data: HashMap<String, String>, cvn: Int? = 10): String {
            val dolBuilder = StringBuilder()
            return when (cvn) {
                10 -> {
                    TlvUtil.readTagList(CVN10_TAGS).forEach {
                        if (it != "9F10") {
                            dolBuilder.append(data[it])
                        } else {
                            dolBuilder.append(data[it]?.substring(6, 14))
                        }
                    }
                    dolBuilder.toString().uppercase()
                }

                17 -> {
                    TlvUtil.readTagList(CVN17_TAGS).forEach {
                        if (it != "9F10") {
                            dolBuilder.append(data[it])
                        } else {
                            dolBuilder.append(data[it]?.substring(8, 10))
                        }
                    }
                    dolBuilder.toString().uppercase()
                }

                else -> {
                    TlvUtil.readTagList(CVN18_TAGS).forEach {
                        dolBuilder.append(data[it])
                    }
                    dolBuilder.toString().uppercase()
                }
            }
        }

        fun getAcPaddingMethod(cvn: Int? = 10): PaddingMethod {
            return when (cvn) {
                10, 17 -> PaddingMethod.ISO9797_1_M1
                else -> PaddingMethod.ISO9797_1_M2
            }
        }

        fun getACCalculationKey(cvn: Int? = 10, pan: String? = null, psn: String? = null, atc: String? = null, un: String? = null): String {
            pan ?: throw Exception("INVALID_ICC_DATA [57]")
            psn ?: throw Exception("INVALID_ICC_DATA [5F34]")
            val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")
            atc ?: throw Exception("INVALID_ICC_DATA [9F36]")
            val sk = EMVUtils.deriveACSessionKey(pan, psn, atc, un) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
            return when (cvn) {
                10 -> iccMK
                17 -> iccMK
                else -> sk
            }
        }
    }

    private val terminalData: HashMap<String, String> = hashMapOf()

    private fun calculateAC(type: ApplicationCryptogram.Type): String {
        val dataBuilder = StringBuilder()
        val cvn = iccData["9F10"]?.let {
            readCVNFromIAD(it)
        } ?: 10
        val pan = iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val psn = iccData["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
        val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")

        return when (type) {
            ApplicationCryptogram.Type.TC,
            ApplicationCryptogram.Type.ARQC -> {
                when (cvn) {
                    10 -> {
                        TlvUtil.readTagList(CVN10_TAGS).forEach {
                            if (it != "9F10") {
                                dataBuilder.append(terminalData[it] ?: iccData[it])
                            } else {
                                dataBuilder.append(iccData[it]?.substring(6, 14))
                            }
                        }
                        Encryption.calculateMAC(iccMK, dataBuilder.toString()).uppercase()
                    }

                    17 -> {
                        TlvUtil.readTagList(CVN17_TAGS).forEach {
                            if (it != "9F10") {
                                dataBuilder.append(terminalData[it] ?: iccData[it])
                            } else {
                                dataBuilder.append(iccData[it]?.substring(8, 10))
                            }
                        }
                        Encryption.calculateMAC(iccMK, dataBuilder.toString()).uppercase()
                    }

                    else -> {
                        val atc = iccData["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
                        val sk = EMVUtils.deriveACSessionKey(pan, psn, atc) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
                        TlvUtil.readTagList(CVN18_TAGS).forEach {
                            dataBuilder.append(terminalData[it] ?: iccData[it])
                        }
                        Encryption.calculateMAC(sk, dataBuilder.toString().applyPadding(PaddingMethod.ISO9797_1_M2)).uppercase()
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
        Log.d("VisaSimulator", "processTerminalData - terminalData: $terminalData")
    }

    override fun onPPSEReply(cAPDU: String): String {
        Log.d("VisaSimulator", "onPPSEReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "325041592E5359532E4444463031",
                    "A5" to mapOf(
                        "BF0C" to mapOf(
                            "61" to mapOf(
                                "4F" to "A0000000031010",
                                "50" to "5649534120435245444954"
                            )
                        )
                    )

                )
            )
        )
        Log.d("VisaSimulator", "onPPSEReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onSelectAIDReply(cAPDU: String): String {
        Log.d("VisaSimulator", "onSelectAIDReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "A0000000031010",
                    "A5" to mapOf(
                        "9F38" to iccData["9F38"],
                        "BF0C" to mapOf(
                            "9F5A" to iccData["9F5A"]
                        )
                    )
                )
            )
        )
        Log.d("VisaSimulator", "onSelectAIDReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onExecuteGPOReply(cAPDU: String): String {
        Log.d("VisaSimulator", "onExecuteGPOReply - cAPDU: $cAPDU")

        processTerminalData(cAPDU)

        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "77" to mapOf(
                    "57" to iccData["57"],
                    "82" to iccData["82"],
                    "5F34" to iccData["5F34"],
                    "9F10" to iccData["9F10"],
                    "9F26" to calculateAC(ApplicationCryptogram.Type.ARQC),
                    "9F27" to getCryptogramInformationData(ApplicationCryptogram.Type.ARQC),
                    "9F36" to iccData["9F36"],
                    "9F6C" to iccData["9F6C"],
                    "9F6E" to iccData["9F6E"],
                )
            )
        )
        Log.d("VisaSimulator", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onReadRecordReply(cAPDU: String): String {
        Log.d("VisaSimulator", "onReadRecordReply - cAPDU: $cAPDU")
        return ""
    }

    override fun onGenerateACReply(cAPDU: String): String {
        Log.d("VisaSimulator", "onGenerateACReply - cAPDU: $cAPDU")
        return ""
    }

    override fun onGetChallengeReply(cAPDU: String): String {
        Log.d("VisaSimulator", "onGenerateACReply - cAPDU: $cAPDU")
        return "${UUidUtil.genHexIdByLength(16)}9000"
    }
}