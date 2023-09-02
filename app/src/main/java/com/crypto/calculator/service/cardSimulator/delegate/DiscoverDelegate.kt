package com.crypto.calculator.service.cardSimulator.delegate

import android.content.Context
import android.util.Log
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.service.cardSimulator.BasicEMVCard
import com.crypto.calculator.service.cardSimulator.BasicEMVService
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class DiscoverDelegate(context: Context, private val iccData: HashMap<String, String>) : BasicEMVCard(context, iccData), BasicEMVService.EMVFlowDelegate {
    companion object {
        fun getInstance(context: Context, iccData: HashMap<String, String>) = DiscoverDelegate(context, iccData)
        const val CVN15_TAGS = "9F029F1A9F379F369F10"
        const val CVN16_TAGS = ""

        fun readCVNFromIAD(iad: String): Int {
            try {
                Log.d("DiscoverDelegate", "readCVNFromIAD - iad: $iad")
                val cvn = iad.substring(2, 4).toInt()
                Log.d("DiscoverDelegate", "readCVNFromIAD - cvn: $cvn")
                return cvn
            } catch (ex: Exception) {
                throw Exception("INVALID_ICC_DATA [9F10]")
            }
        }

        fun getAcDOL(data: HashMap<String, String>, cvn: Int? = 15): String {
            val dolBuilder = StringBuilder()
            return when (cvn) {
                15 -> {
                    TlvUtil.readTagList(CVN15_TAGS).forEach {
                        dolBuilder.append(data[it] ?: "")
                    }
                    dolBuilder.toString().uppercase()
                }

                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }

        fun getAcPaddingMethod(cvn: Int? = 15): PaddingMethod {
            return when (cvn) {
                15 -> PaddingMethod.ISO9797_M2
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }

        fun getACCalculationKey(context: Context, cvn: Int? = 15, pan: String? = null, psn: String? = null, atc: String? = null, un: String? = null): String {
            pan ?: throw Exception("INVALID_ICC_DATA [57]")
            psn ?: throw Exception("INVALID_ICC_DATA [5F34]")
            val imk = EMVUtils.getIssuerMasterKeyByPan(context, pan)
            val iccMK = EMVUtils.deriveICCMasterKey(imk, pan, psn)
            atc ?: throw Exception("INVALID_ICC_DATA [9F36]")
//            un ?: throw Exception("INVALID_TERMINAL_DATA [9F37]")
            val sk = EMVUtils.deriveACSessionKey(PaymentMethod.DISCOVER, iccMK, atc, un)
            return when (cvn) {
                15 -> sk
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }
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

        processTerminalDataFromGPO(cAPDU)

        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "77" to mapOf(
                    "57" to iccData["57"],
                    "82" to iccData["82"],
                    "5F25" to iccData["5F25"],
                    "5F28" to iccData["5F28"],
                    "5F34" to iccData["5F34"],
                    "9F08" to iccData["9F08"],
                    "9F10" to iccData["9F10"],
                    "9F26" to calculateCryptogram(),
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

    override fun readCryptogramVersionNumber(iad: String): Int {
        try {
            Log.d("DiscoverDelegate", "readCVNFromIAD - iad: $iad")
            val cvn = iad.substring(2, 4).toInt()
            Log.d("DiscoverDelegate", "readCVNFromIAD - cvn: $cvn")
            return cvn
        } catch (ex: Exception) {
            throw Exception("INVALID_ICC_DATA [9F10]")
        }
    }

    override fun getCryptogramCalculationDOL(data: HashMap<String, String>, cvn: Int): String {
        val dolBuilder = StringBuilder()
        return when (cvn) {
            15 -> {
                TlvUtil.readTagList(CVN15_TAGS).forEach {
                    dolBuilder.append(data[it])
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
            15 -> PaddingMethod.ISO9797_M2
            else -> {
                // TODO: calculate other CVN
                throw Exception("UNHANDLED CRYPTOGRAM VERSION")
            }
        }
    }

    override fun getCryptogramCalculationKey(cvn: Int, pan: String, psn: String, atc: String, un: String?): String {
        return when (cvn) {
            15 -> getACSessionKey()
            else -> {
                // TODO: calculate other CVN
                throw Exception("UNHANDLED CRYPTOGRAM VERSION")
            }
        }
    }
}