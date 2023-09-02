package com.crypto.calculator.service.cardSimulator.delegate

import android.content.Context
import android.util.Log
import com.crypto.calculator.model.EMVPublicKey
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.service.cardSimulator.BasicEMVCard
import com.crypto.calculator.service.cardSimulator.BasicEMVService
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.service.model.ApplicationCryptogram.getCryptogramInformationData
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil

class MastercardDelegate(context: Context, private val iccData: HashMap<String, String>) : BasicEMVCard(context, iccData), BasicEMVService.EMVFlowDelegate {
    companion object {
        const val CVN10_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"
        fun getInstance(context: Context, iccData: HashMap<String, String>) = MastercardDelegate(context, iccData)
    }

    override val iccDynamicNumber = "F9DFE983F6C08091"
    override val iccPublicKey = EMVPublicKey(
        exponent = "03",
        modulus = "900000000000000000000000001200000000000000000000000000000000480000000000000000000000000000000010800000000000000000000000019800000000000000000000000000000006600000000000000000000000000000000055"
    )
    override var iccPublicRemainder = ""
    override val iccPrivateExponent = "1800000000000000000000000003000000000000000000000000000000000C000000000000000000000000000000000280000000000000000000000000400000000000000000000000000000000100000000000000000000000000000000000B"

    override val issuerPublicKey = EMVPublicKey(
        exponent = "03",
        modulus = "99903295DA9DFA7CB84E664E6500E48A5A1D2EDD3F460BE4AD52066435A644C5A803AE5B829C31B21E81889869BF98D73D9A126F222AC762298808EA0AFB94B33D8FE26AF363FAEACC3B1557CF31F7CCC996E430EA74F6936993C37F638538C075039AD3A8BAF26E44D25FADD3524107"
    )
    override var issuerPublicRemainder = ""
    override val issuerPrivateExponent = "0A3CD02C1FA421C40C497E497E33426F9F9B8BA8598D33FE2DB0228F36C6D16282AAE97D913D9CE9BDC45E708F954E74BFD7124BACF1C90670DD9D8C81BE2F777F30E6F987169E01A6F06A9B3D25006CC2B2D709A1CE121CAB979A37C869B9BF51B2D2106A113DC590EA4BC360784873"

    override val capkIndex = "F1"
    override val capk = EMVPublicKey(
        exponent = "03",
        modulus = "A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02DD1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF98851FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E090642909C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1B43B3D7835088A2FAFA7BE7"
    )
    override val caPrivateExponent = "1ACF7E1FA59A08E11E1E7D603783E24FB18C71F495C20F15E23DF1D06A199D72B89C2B140013099E60EFFD74A23F3744BCB81CAB226D89179AF4DAF1D1CF4BB88BDDEA419E6D3A199AC4D64A55CF85420F45AD07D8A9DC5BB25B06D78F6C6D48FA137128910007A4DFD5B113EF10F828D37D8C7AA1F7ED1E81692B4F019FB80DB925821ED2F6ACCC5BEA7D3F8F2613CEC62A499F941712BA108FAF437AB3D102DDBFDB3BFBB312D644E138D0D90C023B"

    override val iccPublicKeyCertExpiration = "1249"
    override val iccPublicKeyCertSerialNumber = "000001"
    override val issuerPublicKeyCertExpiration = "1249"
    override val issuerPublicKeyCertSerialNumber = "000001"


    override fun readCryptogramVersionNumber(iad: String): Int {
        try {
            Log.d("MastercardDelegate", "readCVNFromIAD - iad: $iad")
            val cvn = iad.substring(2, 4).toInt()
            Log.d("MastercardDelegate", "readCVNFromIAD - cvn: $cvn")
            return cvn
        } catch (ex: Exception) {
            throw Exception("INVALID_ICC_DATA [9F10]")
        }
    }

    override fun getCryptogramCalculationDOL(data: HashMap<String, String>, cvn: Int): String {
        val dolBuilder = StringBuilder()
        return when (cvn) {
            10 -> {
                TlvUtil.readTagList(CVN10_TAGS).forEach {
                    if (it != "9F10") {
                        dolBuilder.append(data[it])
                    } else {
                        dolBuilder.append(data[it]?.substring(4, 16))
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
            10 -> PaddingMethod.ISO9797_M2
            else -> {
                // TODO: calculate other CVN
                throw Exception("UNHANDLED CRYPTOGRAM VERSION")
            }
        }
    }

    override fun getCryptogramCalculationKey(cvn: Int, pan: String, psn: String, atc: String, un: String?): String {
        return when (cvn) {
            10 -> getACSessionKey()
            else -> {
                // TODO: calculate other CVN
                throw Exception("UNHANDLED CRYPTOGRAM VERSION")
            }
        }
    }

    override fun onPPSEReply(cAPDU: String): String {
        Log.d("MastercardDelegate", "onPPSEReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "325041592E5359532E4444463031",
                    "A5" to mapOf(
                        "BF0C" to mapOf(
                            "61" to mapOf(
                                "4F" to "A0000000041010",
                                "87" to "01"
                            )
                        )
                    )

                )
            )
        )
        Log.d("MastercardDelegate", "onPPSEReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onSelectAIDReply(cAPDU: String): String {
        Log.d("MastercardDelegate", "onSelectAIDReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "6F" to mapOf(
                    "84" to "A0000000041010",
                    "A5" to mapOf(
                        "50" to "4D415354455243415244"
                    )
                )
            )
        )
        Log.d("MastercardDelegate", "onSelectAIDReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onExecuteGPOReply(cAPDU: String): String {
        Log.d("MastercardDelegate", "onExecuteGPOReply - cAPDU: $cAPDU")
        val rAPDU = TlvUtil.encodeTLV(
            mapOf(
                "77" to mapOf(
                    "82" to iccData["82"],
                    // hardcoded here to ensure corresponding command will received && ODA will success
                    "94" to "100101011801010020010200" //10010101, 100101011801010020010200 (later use for ODA)
                )
            )
        )
        Log.d("MastercardDelegate", "onExecuteGPOReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onReadRecordReply(cAPDU: String): String {
        Log.d("MastercardDelegate", "onReadRecordReply - cAPDU: $cAPDU")
        val rAPDU = when (cAPDU) {
            "00B2011400" -> {
                TlvUtil.encodeTLV(
                    mapOf(
                        "70" to mapOf(
                            "57" to iccData["57"],
                            "5A" to (iccData["5A"] ?: iccData["57"]?.substringBefore('D')),
                            "5F24" to iccData["5F24"],
                            "5F25" to iccData["5F25"],
                            "5F28" to iccData["5F28"],
                            "5F34" to iccData["5F34"],
                            "8C" to iccData["8C"],
                            "8E" to iccData["8E"],
                            "9F42" to iccData["9F42"],
                            "9F4A" to iccData["9F4A"],
//                            "8D" to "910A8A0295059F37049F4C08",
//                            "9F07" to "FFC0",
//                            "9F08" to "0002",
//                            "9F0D" to "0000000000",
//                            "9F0E" to "0000000000",
//                            "9F0F" to "0000000000",
                        )
                    )
                ).also {
                    processODAData(it)
                    iccData["9F46"] = calculateICCPKCert() ?: ""
                }
            }
            // hardcoded data for CDA
            "00B2011C00" -> {
                TlvUtil.encodeTLV(
                    mapOf(
                        "70" to mapOf(
                            "8F" to capkIndex,
                            "9F32" to issuerPublicKey.exponent,
                            "90" to calculateIssuerPKCert(),
                        )
                    )
                )
            }

            "00B2012400" -> {
                TlvUtil.encodeTLV(
                    mapOf(
                        "70" to mapOf(
                            "92" to issuerPublicRemainder,
                            "9F47" to iccPublicKey.exponent,
                            "9F48" to iccPublicRemainder,
                        )
                    )
                )
            }

            "00B2022400" -> {
                TlvUtil.encodeTLV(
                    mapOf(
                        "70" to mapOf(
                            "9F46" to iccData["9F46"],
                        )
                    )
                )
            }

            else -> "6A82"
        }
        Log.d("MastercardDelegate", "onReadRecordReply - rAPDU: $rAPDU")

        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onGenerateACReply(cAPDU: String): String {
        Log.d("MastercardDelegate", "onGenerateACReply - cAPDU: $cAPDU")
        processTerminalDataFromGenAC(cAPDU)
        transactionData += TlvUtil.encodeTLV(
            mapOf(
                "9F10" to iccData["9F10"],
                "9F27" to getCryptogramInformationData(ApplicationCryptogram.Type.ARQC),
                "9F36" to iccData["9F36"]
            )
        )
        val rAPDU = when (cAPDU.substring(4, 6)) {
            "80" -> TlvUtil.encodeTLV(
                mapOf(
                    "77" to mapOf(
                        "9F10" to iccData["9F10"],
                        "9F26" to calculateCryptogram(),
                        "9F27" to getCryptogramInformationData(ApplicationCryptogram.Type.ARQC),
                        "9F36" to iccData["9F36"]
                    )
                )
            )

            "90" -> TlvUtil.encodeTLV(
                mapOf(
                    "77" to mapOf(
                        "9F10" to iccData["9F10"],
                        "9F27" to getCryptogramInformationData(ApplicationCryptogram.Type.ARQC),
                        "9F36" to iccData["9F36"],
                        "9F4B" to calculateSDAD(ApplicationCryptogram.Type.ARQC)
                    )
                )
            )

            else -> ""
        }
        Log.d("MastercardDelegate", "onGenerateACReply - rAPDU: $rAPDU")
        return "$rAPDU$APDU_RESPONSE_CODE_OK"
    }

    override fun onGetChallengeReply(cAPDU: String): String {
        return "${UUidUtil.genHexIdByLength(16)}9000"
    }
}