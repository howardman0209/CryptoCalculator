package com.crypto.calculator.service.cardSimulator.delegate

import android.util.Log
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.EMVPublicKey
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.model.getExponentLength
import com.crypto.calculator.model.getModulusLength
import com.crypto.calculator.service.cardSimulator.BasicEMVCard
import com.crypto.calculator.service.cardSimulator.BasicEMVService
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.service.model.ApplicationCryptogram.getCryptogramInformationData
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil
import java.security.MessageDigest

class MastercardDelegate(private val iccData: HashMap<String, String>) : BasicEMVCard(iccData), BasicEMVService.EMVFlowDelegate {
    companion object {
        const val CVN10_TAGS = "9F029F039F1A955F2A9A9C9F37829F369F10"
        fun getInstance(iccData: HashMap<String, String>) = MastercardDelegate(iccData)

        fun readCVNFromIAD(iad: String): Int {
            try {
                Log.d("MastercardDelegate", "readCVNFromIAD - iad: $iad")
                val cvn = iad.substring(2, 4).toInt()
                Log.d("MastercardDelegate", "readCVNFromIAD - cvn: $cvn")
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

        fun getAcPaddingMethod(cvn: Int? = 10): PaddingMethod {
            return when (cvn) {
                10 -> PaddingMethod.ISO9797_1_M2
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }

        fun getACCalculationKey(cvn: Int? = 10, pan: String? = null, psn: String? = null, atc: String? = null, un: String? = null): String {
            pan ?: throw Exception("INVALID_ICC_DATA [57]")
            psn ?: throw Exception("INVALID_ICC_DATA [5F34]")
//            val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")
            atc ?: throw Exception("INVALID_ICC_DATA [9F36]")
            un ?: throw Exception("INVALID_TERMINAL_DATA [9F37]")
            val sk = EMVUtils.deriveACSessionKey(pan, psn, atc, un) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
            return when (cvn) {
                10 -> sk
                else -> {
                    // TODO: calculate other CVN
                    throw Exception("UNHANDLED CRYPTOGRAM VERSION")
                }
            }
        }
    }

    private val terminalData: HashMap<String, String> = hashMapOf()
    private var odaData = ""
    private var transactionData = ""
    private val iccDynamicNumber = "F9DFE983F6C08091"
    private val iccPublicKey = EMVPublicKey(
        exponent = "03",
        modulus = "900000000000000000000000001200000000000000000000000000000000480000000000000000000000000000000010800000000000000000000000019800000000000000000000000000000006600000000000000000000000000000000055"
    )
    private var iccPublicRemainder = ""
    private val iccPrivateExponent = "1800000000000000000000000003000000000000000000000000000000000C000000000000000000000000000000000280000000000000000000000000400000000000000000000000000000000100000000000000000000000000000000000B"

    private val issuerPublicKey = EMVPublicKey(
        exponent = "03",
        modulus = "99903295DA9DFA7CB84E664E6500E48A5A1D2EDD3F460BE4AD52066435A644C5A803AE5B829C31B21E81889869BF98D73D9A126F222AC762298808EA0AFB94B33D8FE26AF363FAEACC3B1557CF31F7CCC996E430EA74F6936993C37F638538C075039AD3A8BAF26E44D25FADD3524107"
    )
    private var issuerPublicRemainder = ""
    private val issuerPrivateExponent = "0A3CD02C1FA421C40C497E497E33426F9F9B8BA8598D33FE2DB0228F36C6D16282AAE97D913D9CE9BDC45E708F954E74BFD7124BACF1C90670DD9D8C81BE2F777F30E6F987169E01A6F06A9B3D25006CC2B2D709A1CE121CAB979A37C869B9BF51B2D2106A113DC590EA4BC360784873"

    private val capkIndex = "F1"
    private val capk = EMVPublicKey(
        exponent = "03",
        modulus = "A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02DD1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF98851FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E090642909C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1B43B3D7835088A2FAFA7BE7"
    )
    private val caPrivateExponent = "1ACF7E1FA59A08E11E1E7D603783E24FB18C71F495C20F15E23DF1D06A199D72B89C2B140013099E60EFFD74A23F3744BCB81CAB226D89179AF4DAF1D1CF4BB88BDDEA419E6D3A199AC4D64A55CF85420F45AD07D8A9DC5BB25B06D78F6C6D48FA137128910007A4DFD5B113EF10F828D37D8C7AA1F7ED1E81692B4F019FB80DB925821ED2F6ACCC5BEA7D3F8F2613CEC62A499F941712BA108FAF437AB3D102DDBFDB3BFBB312D644E138D0D90C023B"


    private fun processTerminalData(cAPDU: String) {
        val data = cAPDU.substring(10).dropLast(2)
        transactionData = data
        val cdolMap = iccData["8C"]?.let { TlvUtil.readDOL(it) } ?: throw Exception("INVALID_ICC_DATA [8C]")
        var cursor = 0
        cdolMap.forEach {
            terminalData[it.key] = data.substring(cursor, cursor + it.value.toInt(16) * 2)
            cursor += it.value.toInt(16) * 2
        }
        Log.d("MastercardDelegate", "processTerminalData - terminalData: $terminalData")
    }

    private fun processODAData(rAPDU: String) {
        TlvUtil.decodeTLV(rAPDU).let {
            odaData = TlvUtil.encodeTLV("${it["70"]}")
            Log.d("MastercardDelegate", "processODAData - odaData: $odaData")
        }
    }

    private fun getStaticAuthData(): String {
        var data = ""
        iccData["9F4A"]?.let { tagList ->
            TlvUtil.readTagList(tagList).forEach { tag ->
                data += iccData[tag] ?: terminalData[tag] ?: ""
            }
        }
        return data
    }

    private fun calculateAC(type: ApplicationCryptogram.Type): String {
        val dataBuilder = StringBuilder()
        val cvn = iccData["9F10"]?.let {
            readCVNFromIAD(it)
        } ?: 10
        val pan = iccData["5A"] ?: iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [5A, 57]")
        val psn = iccData["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
//        val iccMK = EMVUtils.deriveICCMasterKey(pan, psn) ?: throw Exception("DERIVE_ICC_MASTER_KEY_ERROR")

        return when (type) {
            ApplicationCryptogram.Type.TC,
            ApplicationCryptogram.Type.ARQC -> {
                when (cvn) {
                    10 -> {
                        val atc = iccData["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
                        val un = terminalData["9F37"] ?: throw Exception("INVALID_TERMINAL_DATA [9F37]")
                        val sk = EMVUtils.deriveACSessionKey(pan, psn, atc, un) ?: throw Exception("DERIVE_AC_SESSION_KEY_ERROR")
                        TlvUtil.readTagList(CVN10_TAGS).forEach {
                            if (it != "9F10") {
                                dataBuilder.append(terminalData[it] ?: iccData[it])
                            } else {
                                dataBuilder.append(iccData[it]?.substring(4, 16))
                            }
                        }
                        Log.d("MastercardDelegate", "DOL - $dataBuilder, sk: $sk")
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

    private fun calculateIssuerPKCert(): String? {
        if (capk.modulus == null) return null
        if (issuerPublicKey.modulus == null) return null

        val dataToHash = StringBuilder()
        dataToHash.append("02")
        dataToHash.append("${iccData["5A"]?.take(8)}")
        dataToHash.append("1249")
        dataToHash.append("000001")
        dataToHash.append("01")
        dataToHash.append("01")
        dataToHash.append(issuerPublicKey.getModulusLength())
        dataToHash.append(issuerPublicKey.getExponentLength())
        if (issuerPublicKey.modulus.length <= capk.modulus.length - 72) {
            dataToHash.append(issuerPublicKey.modulus.padEnd(capk.modulus.length - 72, 'B'))
        } else {
            issuerPublicRemainder = issuerPublicKey.modulus.substring(capk.modulus.length - 72)
            dataToHash.append(issuerPublicKey.modulus.take(capk.modulus.length - 72))
        }
        Log.d("calculateIssuerPKCert", "issuerPublicRemainder: $issuerPublicRemainder")
        dataToHash.append(issuerPublicRemainder)
        dataToHash.append(issuerPublicKey.exponent)
        val hash = getHash(dataToHash.toString())

        val plainCert = StringBuilder()
        plainCert.append("6A")
        plainCert.append("02")
        plainCert.append("${iccData["5A"]?.take(8)}")
        plainCert.append("1249")
        plainCert.append("000001")
        plainCert.append("01")
        plainCert.append("01")
        plainCert.append(issuerPublicKey.getModulusLength())
        plainCert.append(issuerPublicKey.getExponentLength())
        if (issuerPublicKey.modulus.length <= capk.modulus.length - 72) {
            plainCert.append(issuerPublicKey.modulus.padEnd(capk.modulus.length - 72, 'B'))
        } else {
            plainCert.append(issuerPublicKey.modulus.take(capk.modulus.length - 72))
        }
        plainCert.append(hash)
        plainCert.append("BC")
        Log.d("calculateIssuerPKCert", "plainCert: $plainCert")

        return Encryption.doRSA(plainCert.toString(), caPrivateExponent, capk.modulus)
    }

    private fun calculateICCPKCert(): String? {
        if (issuerPublicKey.modulus == null) return null
        if (iccPublicKey.modulus == null) return null

        val dataToHash = StringBuilder()
        dataToHash.append("04")
        dataToHash.append("${iccData["5A"]}FFFF")
        dataToHash.append("1249")
        dataToHash.append("000001")
        dataToHash.append("01")
        dataToHash.append("01")
        dataToHash.append(iccPublicKey.getModulusLength())
        dataToHash.append(iccPublicKey.getExponentLength())
        if (iccPublicKey.modulus.length <= issuerPublicKey.modulus.length - 84) {
            dataToHash.append(iccPublicKey.modulus.padEnd(issuerPublicKey.modulus.length - 84, 'B'))
        } else {
            iccPublicRemainder = iccPublicKey.modulus.substring(issuerPublicKey.modulus.length - 84)
            dataToHash.append(iccPublicKey.modulus.take(issuerPublicKey.modulus.length - 84))
        }
        Log.d("calculateICCPKCert", "iccPublicRemainder: $iccPublicRemainder")
        dataToHash.append(iccPublicRemainder)
        dataToHash.append(iccPublicKey.exponent)
        dataToHash.append("$odaData${getStaticAuthData()}")
        val hash = getHash(dataToHash.toString())

        val plainCert = StringBuilder()
        plainCert.append("6A")
        plainCert.append("04")
        plainCert.append("${iccData["5A"]}FFFF")
        plainCert.append("1249")
        plainCert.append("000001")
        plainCert.append("01")
        plainCert.append("01")
        plainCert.append(iccPublicKey.getModulusLength())
        plainCert.append(iccPublicKey.getExponentLength())
        if (iccPublicKey.modulus.length <= issuerPublicKey.modulus.length - 84) {
            plainCert.append(iccPublicKey.modulus.padEnd(issuerPublicKey.modulus.length - 84, 'B'))
        } else {
            plainCert.append(iccPublicKey.modulus.take(issuerPublicKey.modulus.length - 84))
        }
        plainCert.append(hash)
        plainCert.append("BC")
        Log.d("calculateICCPKCert", "plainCert: $plainCert")

        return Encryption.doRSA(plainCert.toString(), issuerPrivateExponent, issuerPublicKey.modulus)
    }

    private fun calculateSDAD(type: ApplicationCryptogram.Type): String? {
        if (iccPublicKey.modulus == null) return null

        val applicationCryptogram = calculateAC(type)

        val dataToHash = StringBuilder()
        dataToHash.append("05")
        dataToHash.append("01")
        val iccDynamicDataLength = 29 + iccDynamicNumber.length / 2 + 1
        dataToHash.append(iccDynamicDataLength.toHexString()) // 26H = 38D = 01 + 08 + 01 + 08 + 20
        dataToHash.append((iccDynamicNumber.length / 2).toHexString()) // 01
        dataToHash.append(iccDynamicNumber) // 08
        dataToHash.append(getCryptogramInformationData(type)) // 01
        dataToHash.append(applicationCryptogram) // 08
        dataToHash.append(getHash(transactionData)) // 20
        dataToHash.append("B".repeat((iccPublicKey.modulus.length / 2 - iccDynamicDataLength - 25) * 2))
        dataToHash.append(terminalData["9F37"])
        val hash = getHash(dataToHash.toString())

        val plainCert = StringBuilder()
        plainCert.append("6A")
        plainCert.append("05")
        plainCert.append("01")
        plainCert.append(iccDynamicDataLength.toHexString()) // 26H = 38D = 01 + 08 + 01 + 08 + 20
        plainCert.append((iccDynamicNumber.length / 2).toHexString()) // 01
        plainCert.append(iccDynamicNumber) // 08
        plainCert.append(getCryptogramInformationData(type)) // 01
        plainCert.append(applicationCryptogram) // 08
        plainCert.append(getHash(transactionData)) // 20
        plainCert.append("B".repeat((iccPublicKey.modulus.length / 2 - iccDynamicDataLength - 25) * 2))
        plainCert.append(hash)
        plainCert.append("BC")
        Log.d("calculateSDAD", "plainCert: $plainCert")

        return Encryption.doRSA(plainCert.toString(), iccPrivateExponent, iccPublicKey.modulus)
    }

    private fun getHash(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val message = plaintext.hexToByteArray()
        val cipher = md.digest(message).toHexString().uppercase()
        Log.d("getHash", "plaintext: $plaintext -> cipher: $cipher")
        return cipher
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
                            "5A" to iccData["5A"],
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
        processTerminalData(cAPDU)
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
                        "9F26" to calculateAC(ApplicationCryptogram.Type.ARQC),
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