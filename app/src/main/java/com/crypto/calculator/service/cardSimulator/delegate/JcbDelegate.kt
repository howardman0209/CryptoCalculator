package com.crypto.calculator.service.cardSimulator.delegate

import android.util.Log
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.service.cardSimulator.BasicEMVCard
import com.crypto.calculator.service.cardSimulator.BasicEMVService
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class JcbDelegate(private val iccData: HashMap<String, String>) : BasicEMVCard(iccData), BasicEMVService.EMVFlowDelegate {
    companion object {
        fun getInstance(iccData: HashMap<String, String>) = JcbDelegate(iccData)
        const val CVN01_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"

        fun readCVNFromIAD(iad: String): Int {
            try {
                val cvn = iad.substring(4, 6).toInt(16)
                Log.d("JcbDelegate", "readCVNFromIAD - cvn: $cvn")
                return cvn
            } catch (ex: Exception) {
                throw Exception("INVALID_ICC_DATA [9F10]")
            }
        }

        fun getAcDOL(data: HashMap<String, String>, cvn: Int? = 1): String {
            val dolBuilder = StringBuilder()
            return when (cvn) {
                1 -> {
                    TlvUtil.readTagList(CVN01_TAGS).forEach {
                        if (it != "9F10") {
                            dolBuilder.append(data[it])
                        } else {
                            dolBuilder.append(data[it]?.substring(6))
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
                1 -> PaddingMethod.ISO9797_1_M1
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }

        fun getACCalculationKey(cvn: Int? = 1, pan: String? = null, psn: String? = null, atc: String? = null, un: String? = null): String {
            pan ?: throw Exception("INVALID_ICC_DATA [57]")
            psn ?: throw Exception("INVALID_ICC_DATA [5F34]")
            val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")
            atc ?: throw Exception("INVALID_ICC_DATA [9F36]")
//            un ?: throw Exception("INVALID_TERMINAL_DATA [9F37]")
//            val sk = EMVUtils.deriveACSessionKey(pan, psn, atc, un) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
            return when (cvn) {
                1 -> iccMK
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }
    }

    override fun onPPSEReply(cAPDU: String): String {
        Log.d("JcbDelegate", "onPPSEReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "325041592E5359532E4444463031",
                    "A5" to mapOf(
                        "BF0C" to mapOf(
                            "61" to mapOf(
                                "4F" to "A0000000651010",
                                "50" to "4A434220437265646974",
                                "87" to "01"
                            )
                        )
                    )

                )
            )
        )
        Log.d("JcbDelegate", "onPPSEReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onSelectAIDReply(cAPDU: String): String {
        Log.d("JcbDelegate", "onSelectAIDReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "A0000000651010",
                    "A5" to mapOf(
                        "50" to "4A434220437265646974",
                        "87" to "01",
                        "9F38" to iccData["9F38"],
                    )
                )
            )
        )
        Log.d("JcbDelegate", "onSelectAIDReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onExecuteGPOReply(cAPDU: String): String {
        Log.d("JcbDelegate", "onExecuteGPOReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "80" to "${iccData["82"]}08010100" // hardcoded AFL Tag[94]
            )
        )
        Log.d("JcbDelegate", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onReadRecordReply(cAPDU: String): String {
        Log.d("JcbDelegate", "onReadRecordReply - cAPDU: $cAPDU")
        val rAPDU = when (cAPDU) {

            "00B2010C00" -> TlvUtil.encodeTLV(
                mapOf(
                    "70" to mapOf(
                        "57" to iccData["57"],
                        "5A" to iccData["5A"],
                        "5F24" to iccData["5F24"],
                        "5F34" to iccData["5F34"],
                        "5F28" to iccData["5F28"],
                        "8C" to iccData["8C"],
                        "8E" to iccData["8E"],
                        "9F42" to iccData["9F42"],
                    )
                )
            )

            else -> "6A82"
        }
        Log.d("JcbDelegate", "onReadRecordReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onGenerateACReply(cAPDU: String): String {
        Log.d("JcbDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        processTerminalDataFromGenAC(cAPDU)

        val sb = StringBuilder()
        sb.append(ApplicationCryptogram.getCryptogramInformationData(ApplicationCryptogram.Type.ARQC))
        sb.append(iccData["9F36"])
        sb.append(calculateCryptogram())
        sb.append(iccData["9F10"])
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "80" to sb.toString()
            )
        )
        Log.d("JcbDelegate", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onGetChallengeReply(cAPDU: String): String {
        Log.d("JcbDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        return "${UUidUtil.genHexIdByLength(16)}9000"
    }

    override fun readCryptogramVersionNumber(iad: String): Int {
        try {
            val cvn = iad.substring(4, 6).toInt(16)
            Log.d("JcbDelegate", "readCVNFromIAD - cvn: $cvn")
            return cvn
        } catch (ex: Exception) {
            throw Exception("INVALID_ICC_DATA [9F10]")
        }
    }

    override fun getCryptogramCalculationDOL(data: HashMap<String, String>, cvn: Int): String {
        val dolBuilder = StringBuilder()
        return when (cvn) {
            1 -> {
                TlvUtil.readTagList(CVN01_TAGS).forEach {
                    if (it != "9F10") {
                        dolBuilder.append(data[it])
                    } else {
                        dolBuilder.append(data[it]?.substring(6))
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

    override fun getCryptogramCalculationPadding(cvn: Int): PaddingMethod {
        return when (cvn) {
            1 -> PaddingMethod.ISO9797_1_M1
            else -> {
                // TODO: calculate other CVN
                throw Exception("UNHANDLED CRYPTOGRAM VERSION")
            }
        }
    }

    override fun getCryptogramCalculationKey(cvn: Int, pan: String, psn: String, atc: String, un: String?): String {
        val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")
        return when (cvn) {
            1 -> iccMK
            else -> {
                // TODO: calculate other CVN
                throw Exception("UNHANDLED CRYPTOGRAM VERSION")
            }
        }
    }
}