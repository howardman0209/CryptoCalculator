package com.crypto.calculator.cardReader

import android.content.Context
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.extension.hexToBinary
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.EMVPublicKey
import com.crypto.calculator.model.EMVTags
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.model.getHexTag
import com.crypto.calculator.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.crypto.calculator.util.APDU_COMMAND_GET_CHALLENGE
import com.crypto.calculator.util.APDU_COMMAND_GPO_WITHOUT_PDOL
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.CVM_NO_CVM_BINARY_CODE
import com.crypto.calculator.util.CVM_SIGNATURE_BINARY_CODE
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.PreferencesUtil
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.UUidUtil
import java.security.MessageDigest

class EMVKernel(val context: Context, nfcDelegate: NfcDelegate) : BasicEMVKernel(nfcDelegate) {
    companion object {
        val apdu: MutableLiveData<String?> = MutableLiveData()
    }

    override fun onCommunication(isoDep: IsoDep) {
        super.onCommunication(isoDep)
        ppse(isoDep)
        selectAID(isoDep)
        kernelSpecialDataHandling()
        executeGPO(isoDep)
        readRecord(isoDep)
        when (EMVUtils.getPaymentMethodByAID(getICCTag(EMVTags.APPLICATION_IDENTIFIER_CARD.getHexTag()) ?: "")) {
            PaymentMethod.VISA,
            PaymentMethod.DISCOVER,
            PaymentMethod.DINERS,
            PaymentMethod.UNIONPAY -> {
                Log.d("EMVKernel", "skip generate AC")
            }

            else -> generateAC(isoDep)
        }
        performODA()
        performCVM()
        nfcDelegate.onCardDataReceived(getICCData() + getTerminalData())
    }

    override fun communicator(isoDep: IsoDep, cmd: String): String {
        val cAPDU = cmd.uppercase()
        apdu.value = cAPDU
        val rAPDU = super.communicator(isoDep, cmd)
        apdu.postValue(rAPDU)
        return rAPDU
    }

    override fun ppse(isoDep: IsoDep) {
        val tlv = communicator(isoDep, APDU_COMMAND_2PAY_SYS_DDF01).let {
            if (it.endsWith(APDU_RESPONSE_CODE_OK)) {
                it
            } else {
                throw Exception("Card not supported")
//                communicator(isoDep, APDU_COMMAND_1PAY_SYS_DDF01)
            }
        }

        val appTemplates = TlvUtil.findByTag(tlv, tag = EMVTags.APPLICATION_TEMPLATE.getHexTag())
        Log.d("ppse", "appTemplates: $appTemplates")
        val finalTlv = appTemplates?.let { appList ->
            // check if more than 1 aid return
            if (appList.size > 1) {
                Log.d("ppse", "multiple AID read from card")
                // CTL -> auto select app with higher Application Priority Indicator
                appList.minBy { TlvUtil.decodeTLV(it)[EMVTags.APPLICATION_PRIORITY_INDICATOR.getHexTag()].toString().toInt(16) } ?: appList.first()
            } else {
                Log.d("ppse", "single AID read from card")
                appList.first()
            }
        }
        Log.d("ppse", "finalTlv: $finalTlv")
        finalTlv?.let {
            processTlv(it)
        }
    }

    override fun selectAID(isoDep: IsoDep) {
        getICCTag(EMVTags.APPLICATION_IDENTIFIER_CARD.getHexTag())?.also { aid ->
            val cla = "00"
            val ins = "A4"
            val p1 = "04"
            val p2 = "00"
            val aidSize = (aid.length / 2).toHexString()
            Log.d("selectAID", "aidSize: $aidSize")
            val le = "00"
            val apduCommand = "$cla$ins$p1$p2$aidSize$aid$le"

            val tlv = communicator(isoDep, apduCommand)
            processTlv(tlv)

            getICCTag(EMVTags.APPLICATION_PROGRAM_IDENTIFIER.getHexTag())?.let {
                processTlv("${EMVTags.APPLICATION_CURRENCY_CODE.getHexTag()}02${it.substring(2, 6)}")
                processTlv("${EMVTags.ISSUER_COUNTRY_CODE.getHexTag()}02${it.substring(6)}")
            }
        } ?: run {
            throw Exception("AID_NOT_FOUND")
        }
    }

    private fun kernelSpecialDataHandling() {
        getICCTag(EMVTags.APPLICATION_IDENTIFIER_CARD.getHexTag())?.also { aid ->
            when (EMVUtils.getPaymentMethodByAID(aid)) {
                PaymentMethod.AMEX -> {
                    saveTerminalData(
                        hashMapOf(
                            EMVTags.TERMINAL_TYPE.getHexTag() to "E2" // E2 for AMEX
                        )
                    )
                }

                else -> {}
            }
        }
    }

    override fun executeGPO(isoDep: IsoDep) {
        val tlv = getICCTag(EMVTags.PDOL.getHexTag())?.let { pdol ->
            val cla = "80"
            val ins = "A8"
            val p1 = "00"
            val p2 = "00"

            val pdolMap = TlvUtil.readDOL(pdol)
            Log.d("executeGPO", "pdolMap: $pdolMap")
            val sb = StringBuilder()
            pdolMap.forEach {
                when (it.key) {
                    EMVTags.ICC_DYNAMIC_NUMBER.getHexTag() -> {
                        val iccDynamicNumber = getChallenge(isoDep)
                        Log.d("executeGPO", "iccDynamicNumber: $iccDynamicNumber")
                        sb.append(iccDynamicNumber ?: "00".repeat(it.value.toInt(16)))
                    }

                    EMVTags.UNPREDICTABLE_NUMBER.getHexTag() -> sb.append(getUnpredictableNum(it.value.toInt(16) * 2))
                    else -> sb.append(getICCTag(it.key) ?: getTerminalTag(it.key) ?: "00".repeat(it.value.toInt(16)))
                }
                Log.d("executeGPO", "${it.key}: $sb")
            }

            val data = sb.toString()
            val lc = (data.length / 2 + 2).toHexString()
            Log.d("executeGPO", "lc: $lc")

            val fixByte = "83"

            val dataSizeHex = (data.length / 2).toHexString()
            Log.d("executeGPO", "dataSizeHex: $dataSizeHex")

            val le = "00"

            val apduCommand = "$cla$ins$p1$p2$lc$fixByte$dataSizeHex$data$le"
            communicator(isoDep, apduCommand)
        } ?: run {
            communicator(isoDep, APDU_COMMAND_GPO_WITHOUT_PDOL)
        }

        // handle Format 1 data
        if (tlv.startsWith(EMVTags.RESPONSE_MESSAGE_TEMPLATE_1.getHexTag())) {
            val tag80Data = TlvUtil.findByTag(tlv, EMVTags.RESPONSE_MESSAGE_TEMPLATE_1.getHexTag())?.first()
            tag80Data?.let {
                val aip = it.substring(0, 4) // AIP
                processTlv("${EMVTags.APPLICATION_INTERCHANGE_PROFILE.getHexTag()}02$aip")
                val afl = it.substring(4)// AFL
                val aflLength = (afl.length / 2).toHexString()
                processTlv("${EMVTags.APPLICATION_FILE_LOCATOR.getHexTag()}$aflLength$afl")
            }
        } else {
            processTlv(tlv)
        }
    }

    override fun readRecord(isoDep: IsoDep) {
        val cla = "00"
        val ins = "B2"
        val le = "00"
        getICCTag(EMVTags.APPLICATION_FILE_LOCATOR.getHexTag())?.also { afl ->
            if (afl.isNotEmpty()) {
                val locators = afl.chunked(8)
                Log.d("readRecord", "locators: $locators")

                locators.forEach {
                    val sfi = it.take(2)
                    Log.d("readRecord", "sfi: $sfi")
                    val p2 = ("${sfi.hexToBinary().take(5)}000".toInt(2) + "0100".toInt(2)).toHexString()
                    val firstRecord = it.substring(2, 4).toInt(16)
                    val lastRecord = it.substring(4, 6).toInt(16)
                    var odaLabel = it.substring(7).toInt(16)
                    Log.d("readRecord", "p2: $p2, firstRecord: $firstRecord, lastRecord: $lastRecord, odaLabel: $odaLabel")
                    for (i in firstRecord..lastRecord) {
                        val p1 = String.format("%02d", i)
                        val cmd = "$cla$ins$p1$p2$le"
                        val tlv = communicator(isoDep, cmd)
                        Log.d("readRecord", "tlv: $tlv")
                        if (tlv.endsWith(APDU_RESPONSE_CODE_OK)) {
                            if (odaLabel > 0) {
                                saveOdaData(TlvUtil.findByTag(tlv, EMVTags.EMV_PROPRIETARY_TEMPLATE.getHexTag())?.first() ?: "")
                                odaLabel--
                            }
                            processTlv(tlv)
                        }
                    }
                }
                Log.d("readRecord", "odaData: ${getOdaData()}")
            }
        } ?: run {
            Log.d("readRecord", "AFL_NOT_FOUND")
//            throw Exception("AFL_NOT_FOUND")
        }
    }

    override fun generateAC(isoDep: IsoDep) {
        val cla = "80"
        val ins = "AE"
        val isCardSupportCDA = getICCTag(EMVTags.APPLICATION_INTERCHANGE_PROFILE.getHexTag())?.hexToBinary()?.get(7) == '1'
        // 00:(AAC), 40:(TC) (offline transaction), 80:(ARQC)
        val p1 = if (isCardSupportCDA) ("80".toInt(16) + "10000".toInt(2)).toHexString() else "80"
        val p2 = "00"

        getICCTag(EMVTags.CDOL.getHexTag())?.also { cdol ->
            val cdolMap = TlvUtil.readDOL(cdol)
            Log.d("generateAC", "cdolMap: $cdolMap")
            val sb = StringBuilder()
            cdolMap.forEach {
                when (it.key) {
                    EMVTags.ICC_DYNAMIC_NUMBER.getHexTag() -> {
                        val iccDynamicNumber = getChallenge(isoDep)
                        Log.d("generateAC", "iccDynamicNumber: $iccDynamicNumber")
                        sb.append(iccDynamicNumber ?: "00".repeat(it.value.toInt(16)))
                    }

                    EMVTags.UNPREDICTABLE_NUMBER.getHexTag() -> sb.append(getUnpredictableNum(it.value.toInt(16) * 2))
                    else -> sb.append(getICCTag(it.key) ?: getTerminalTag(it.key) ?: "00".repeat(it.value.toInt(16)))
                }
            }

            val data = sb.toString()
            val lc = (data.length / 2).toHexString()
            Log.d("generateAC", "lc: $lc")

            val le = "00"

            val apduCommand = "$cla$ins$p1$p2$lc$data$le"
            val tlv = communicator(isoDep, apduCommand)

            // handle Format 1 data
            if (tlv.startsWith(EMVTags.RESPONSE_MESSAGE_TEMPLATE_1.getHexTag())) {
                val tag80Data = TlvUtil.findByTag(tlv, tag = EMVTags.RESPONSE_MESSAGE_TEMPLATE_1.getHexTag())?.first()
                tag80Data?.let {
                    val cid = it.substring(0, 2)
                    processTlv("${EMVTags.CRYPTOGRAM_INFORMATION_DATA.getHexTag()}01$cid")
                    val atc = it.substring(2, 6)
                    processTlv("${EMVTags.APPLICATION_TRANSACTION_COUNTER.getHexTag()}02$atc")
                    val ac = it.substring(6, 22)
                    processTlv("${EMVTags.APPLICATION_CRYPTOGRAM.getHexTag()}08$ac")
                    val iad = it.substring(22)
                    val iadLength = (iad.length / 2).toHexString()
                    processTlv("${EMVTags.ISSUER_APPLICATION_DATA.getHexTag()}$iadLength$iad")
                }
            } else {
                processTlv(tlv)
            }
        } ?: run {
            throw Exception("CDOL_NOT_FOUND")
        }
    }

    private fun getUnpredictableNum(length: Int): String {
//        Log.d("getUnpredictableNum", "length: $length")
        val unpredictableNum = UUidUtil.genHexIdByLength(length).uppercase()
        Log.d("getUnpredictableNum", "unpredictable number: $unpredictableNum")
        val tmp = hashMapOf(EMVTags.UNPREDICTABLE_NUMBER.getHexTag() to unpredictableNum)
        saveTerminalData(tmp)
        return unpredictableNum
    }

    override fun getChallenge(isoDep: IsoDep): String? {
        val tlv = communicator(isoDep, APDU_COMMAND_GET_CHALLENGE)
        val challenge = if (tlv.endsWith(APDU_RESPONSE_CODE_OK)) {
            tlv.dropLast(4)
        } else null
        Log.d("getChallenge", "challenge: $challenge")
        return challenge
    }

    override fun performCVM() {
        // Assume terminal only support signature
        getICCTag(EMVTags.CVM_LIST.getHexTag())?.let { cvmList ->
            cvmKernel2(cvmList)
        } ?: run {
            cvmKernel3()
        }
    }

    private fun cvmKernel2(cvmList: String) {
        //Kernel 2 (Mastercard)
        Log.d("performCVM", "cvmList: $cvmList")
        val amountX = cvmList.substring(0, 8).toInt(16)
        val amountY = cvmList.substring(8, 16).toInt(16)
        val cvRules = cvmList.substring(16).chunked(4)
        Log.d("performCVM", "amountX: $amountX, amountY: $amountY, cvRules: $cvRules")

        val isTerminalSupportCDCVM = getTerminalTag(EMVTags.TERMINAL_TRANSACTION_QUALIFIERS.getHexTag())?.let { ttq ->
            ttq.hexToBinary()[17] == '1'
        } ?: false

        val isCardSupportCDCVM = getICCTag(EMVTags.APPLICATION_INTERCHANGE_PROFILE.getHexTag())?.let { aip ->
            aip.hexToBinary()[6] == '1'
        } ?: false

        val exceededCVMRequiredLimit = getTerminalTag(EMVTags.READER_CVM_REQUIRED_LIMIT.getHexTag())?.toInt()?.let { cvmRequiredLimit ->
            getTerminalTag(EMVTags.AUTHORISED_AMOUNT.getHexTag())?.toInt()?.let { it >= cvmRequiredLimit }
                ?: throw Exception("INVALID_AUTHORISED_AMOUNT")
        } ?: false

        if (isTerminalSupportCDCVM && isCardSupportCDCVM) {
            if (exceededCVMRequiredLimit) {
                // CDCVM success -> CVM result = 010002
                Log.d("performCVM", "Card Verification Method: CDCVM - SUCCESS")
                saveTerminalData(mapOf(EMVTags.CVM_RESULTS.getHexTag() to "010002"))
            } else {
                Log.d("performCVM", "Card Verification Method: No CVM - SUCCESS")
                saveTerminalData(mapOf(EMVTags.CVM_RESULTS.getHexTag() to "3F0002"))
            }
        } else {
            cvRules.forEach { rule ->
                val cvmCode = rule.take(2)
                val conditionCode = rule.takeLast(2)
//            Log.d("performCVM", "conditionCode: $conditionCode")
                val haveSucceeding = cvmCode.hexToBinary()[1] == '1'

                val matchCondition = when (conditionCode) {
                    "00" -> true
                    "03" -> {
                        when {
                            cvmCode.hexToBinary().takeLast(6) == CVM_SIGNATURE_BINARY_CODE -> true
                            cvmCode.hexToBinary().takeLast(6) == CVM_NO_CVM_BINARY_CODE -> true
                            cvmCode.hexToBinary().takeLast(6).toInt(2) in 1..5 -> false // TODO: Support pin
                            else -> false
                        }
                    }

                    "05" -> getTerminalTag(EMVTags.AMOUNT_OTHER.getHexTag()) != "000000000000"
                    "06" -> getTerminalTag(EMVTags.TRANSACTION_CURRENCY_CODE.getHexTag()) == getTerminalTag(EMVTags.APPLICATION_CURRENCY_CODE.getHexTag())
                            && getTerminalTag(EMVTags.AUTHORISED_AMOUNT.getHexTag())?.toInt()?.let { it < amountX } ?: throw Exception("INVALID_AUTHORISED_AMOUNT")

                    "07" -> getTerminalTag(EMVTags.TRANSACTION_CURRENCY_CODE.getHexTag()) == getTerminalTag(EMVTags.APPLICATION_CURRENCY_CODE.getHexTag())
                            && getTerminalTag(EMVTags.AUTHORISED_AMOUNT.getHexTag())?.toInt()?.let { it >= amountX } ?: throw Exception("INVALID_AUTHORISED_AMOUNT")

                    "08" -> getTerminalTag(EMVTags.TRANSACTION_CURRENCY_CODE.getHexTag()) == getTerminalTag(EMVTags.APPLICATION_CURRENCY_CODE.getHexTag())
                            && getTerminalTag(EMVTags.AUTHORISED_AMOUNT.getHexTag())?.toInt()?.let { it < amountY } ?: throw Exception("INVALID_AUTHORISED_AMOUNT")

                    "09" -> getTerminalTag(EMVTags.TRANSACTION_CURRENCY_CODE.getHexTag()) == getTerminalTag(EMVTags.APPLICATION_CURRENCY_CODE.getHexTag())
                            && getTerminalTag(EMVTags.AUTHORISED_AMOUNT.getHexTag())?.toInt()?.let { it >= amountY } ?: throw Exception("INVALID_AUTHORISED_AMOUNT")

                    else -> false
                }
                Log.d("performCVM", "Current CV rule: $cvmCode, match condition: $matchCondition")

                if (!matchCondition) {
                    return@forEach
                }

                when {
                    cvmCode.hexToBinary().takeLast(6) == CVM_SIGNATURE_BINARY_CODE -> {
                        Log.d("performCVM", "Card Verification Method: Signature - SUCCESS")
                        saveTerminalData(mapOf(EMVTags.CVM_RESULTS.getHexTag() to "$cvmCode${conditionCode}02"))
                        return
                    }

                    cvmCode.hexToBinary().takeLast(6) == CVM_NO_CVM_BINARY_CODE -> {
                        Log.d("performCVM", "Card Verification Method: No CVM - SUCCESS")
                        saveTerminalData(mapOf(EMVTags.CVM_RESULTS.getHexTag() to "$cvmCode${conditionCode}02"))
                        return
                    }

                    cvmCode.hexToBinary().takeLast(6).toInt(2) in 1..5 -> {
                        Log.d("performCVM", "PIN not support, apply succeeding: $haveSucceeding")
                        //TODO: Support pin
                        if (!haveSucceeding) {
                            saveTerminalData(mapOf(EMVTags.CVM_RESULTS.getHexTag() to "$cvmCode${conditionCode}02"))
                            return
                        }
                    }

                    else -> {
                        if (!haveSucceeding) {
                            Log.d("performCVM", "undefined CVM - FAIL")
                            saveTerminalData(mapOf(EMVTags.CVM_RESULTS.getHexTag() to "$cvmCode${conditionCode}01"))
                            return
                        }
                    }
                }
            }
        }
    }

    private fun cvmKernel3() {
        //Kernel 3 (Visa)
        var isTerminalSupportOnlinePin = false
        var isTerminalSupportSignature = false
        var isTerminalSupportCDCVM = false
        getTerminalTag(EMVTags.TERMINAL_TRANSACTION_QUALIFIERS.getHexTag())?.also { ttq ->
            Log.d("performCVM", "ttq: $ttq, (${ttq.hexToBinary()})")
            isTerminalSupportOnlinePin = ttq.hexToBinary()[5] == '1'
            isTerminalSupportSignature = ttq.hexToBinary()[6] == '1'
            isTerminalSupportCDCVM = ttq.hexToBinary()[17] == '1'
        }
        Log.d("performCVM", "Terminal Transaction Qualifiers:\n Online Pin: $isTerminalSupportOnlinePin \n Signature: $isTerminalSupportSignature \n CDCVM: $isTerminalSupportCDCVM")

        getICCTag(EMVTags.CARD_TRANSACTION_QUALIFIERS.getHexTag())?.also { ctq ->
            Log.d("performCVM", "ctq: $ctq, (${ctq.hexToBinary()})")
            val isCardRequireOnlinePin = ctq.hexToBinary()[0] == '1'
            val isCardRequireSignature = ctq.hexToBinary()[1] == '1'
            val isCardPerformedCDCVM = ctq.hexToBinary()[8] == '1'
            Log.d("performCVM", "Card Transaction Qualifiers:\n Online Pin: $isCardRequireOnlinePin \n Signature: $isCardRequireSignature \n CDCVM: $isCardPerformedCDCVM")

            when {
                isCardRequireOnlinePin && isTerminalSupportOnlinePin -> {
                    // TODO: Support online pin
                }

                isCardPerformedCDCVM && isTerminalSupportCDCVM -> {
                    getICCTag(EMVTags.CARD_AUTHENTICATION_RELATED_DATA.getHexTag())?.also {
                        if (it.takeLast(4) == ctq) {
                            Log.d("performCVM", "Card Verification Method: CDCVM - SUCCESS")
                        } else {
                            Log.d("performCVM", "Card Verification Method: CDCVM - FAIL")
                        }
                    } ?: run {
                        //check application cryptogram is ARQC
                        if (getICCTag(EMVTags.CRYPTOGRAM_INFORMATION_DATA.getHexTag()) == "80") {
                            Log.d("performCVM", "Card Verification Method: CDCVM - SUCCESS")
                        } else {
                            Log.d("performCVM", "Card Verification Method: CDCVM - FAIL")
                        }
                    }
                }

                isCardRequireSignature && isTerminalSupportSignature -> {
                    Log.d("performCVM", "Card Verification Method: Signature - SUCCESS")
                }

                else -> {
                    Log.d("performCVM", "Card Verification Method: No CVM - FAIL")
                }
            }
        } ?: run {
            Log.d("performCVM", "no ctq returned")
            when {
                isTerminalSupportSignature -> {
                    Log.d("performCVM", "Card Verification Method: Signature - SUCCESS")
                }

                isTerminalSupportCDCVM && isTerminalSupportOnlinePin -> { // CDCVM is mandatory
                    // TODO: Support online pin
                }

                else -> {
                    Log.d("performCVM", "Card Verification Method: No CVM - FAIL")
                }
            }
        }
    }

    override fun performODA() {
        var isTerminalSupportSDA = false
        var isTerminalSupportDDA = false
        var isTerminalSupportCDA = false
        getTerminalTag(EMVTags.TERMINAL_CAPABILITIES.getHexTag())?.also { termCap ->
            Log.d("performODA", "termCap: $termCap, (${termCap.hexToBinary()})")
            isTerminalSupportSDA = termCap.hexToBinary()[16] == '1'
            isTerminalSupportDDA = termCap.hexToBinary()[17] == '1'
            isTerminalSupportCDA = termCap.hexToBinary()[20] == '1'
        }
        Log.d("performODA", "Terminal Capabilities:\n SDA: $isTerminalSupportSDA \n DDA: $isTerminalSupportDDA \n CDA: $isTerminalSupportCDA")

        getICCTag(EMVTags.APPLICATION_INTERCHANGE_PROFILE.getHexTag())?.also { aip ->
            Log.d("performODA", "aip: $aip")
            val isCardSupportSDA = aip.hexToBinary()[1] == '1'
            val isCardSupportDDA = aip.hexToBinary()[2] == '1'
            val isCardSupportCDA = aip.hexToBinary()[7] == '1'
            Log.d("performODA", "Application Interchange Profile:\n SDA: $isCardSupportSDA \n DDA: $isCardSupportDDA \n CDA: $isCardSupportCDA")
            when {
                isCardSupportCDA && isTerminalSupportCDA -> dynamicDataAuthentication(true).also { result ->
                    if (result) Log.d("performODA", "CDA success") else Log.d("performODA", "CDA Fail")
                }

                isCardSupportDDA && isTerminalSupportDDA -> dynamicDataAuthentication().also { result ->
                    if (result) Log.d("performODA", "DDA success") else Log.d("performODA", "DDA Fail")
                }

                isCardSupportSDA && isTerminalSupportSDA -> staticDataAuthentication().also { result ->
                    if (result) Log.d("performODA", "SDA success") else Log.d("performODA", "SDA Fail")
                }

                else -> {
                    Log.d("performODA", "ODA not performed")
                    // TODO: set TVR: ODA not performed
                }
            }
        } ?: throw Exception("AIP_NOT_FOUND")
    }

    private fun dynamicDataAuthentication(isCDA: Boolean = false): Boolean {
        val issuerPK = retrieveIssuerPK() ?: run {
            Log.d("_DDA/_CDA", "Retrieve Issuer Public Key Fail")
            return false
        }
        Log.d("_DDA/_CDA", "issuerPK: ${issuerPK.exponent}, ${issuerPK.modulus}")

        val iccPK = retrieveIccPK(issuerPK) ?: run {
            Log.d("_DDA/_CDA", "Retrieve ICC Public Key Fail")
            return false
        }
        Log.d("_DDA/_CDA", "iccPK: ${iccPK.exponent}, ${iccPK.modulus}")

        val sdad = getICCTag(EMVTags.SIGNED_DYNAMIC_APPLICATION_DATA.getHexTag()) // returned from GPO
        val decryptedSDAD = sdad?.let {
            // fDDA
            iccPK.exponent ?: return false
            iccPK.modulus ?: return false
            Encryption.doRSA(it, iccPK.exponent, iccPK.modulus)
        } ?: return false
        Log.d("_DDA/_CDA", "decryptedSDAD: $decryptedSDAD")
        if (!verifySDAD(decryptedSDAD)) return false
        Log.d("_DDA/_CDA", "verifySDAD SUCCESS")

        if (isCDA) saveIccDynamicData(decryptedSDAD) // retrieve and save Application Cryptogram [9F26]

        Log.d("_DDA/_CDA", "Dynamic Data Authentication SUCCESS, CDA: $isCDA")
        return true
    }

    private fun saveIccDynamicData(decryptedSDAD: String) {
        val length = decryptedSDAD.substring(6, 8).toInt(16) * 2
        val iccDynamicData = decryptedSDAD.substring(8, 8 + length)
        val iccDynamicNumberLength = iccDynamicData.substring(0, 2).toInt(16) * 2
        val iccDynamicNumber = iccDynamicData.substring(2, 2 + iccDynamicNumberLength)
        Log.d("saveIccDynamicData", "iccDynamicNumber: $iccDynamicNumber")
        val cid = iccDynamicData.substring(2 + iccDynamicNumberLength, 4 + iccDynamicNumberLength)
        Log.d("saveIccDynamicData", "cid: $cid")
        val ac = iccDynamicData.substring(4 + iccDynamicNumberLength, 20 + iccDynamicNumberLength)
        val txnDataHashCode = iccDynamicData.substring(iccDynamicData.length - 40, iccDynamicData.length)
        Log.d("saveIccDynamicData", "txnDataHashCode: $txnDataHashCode")
        // add cryptogram to tlv
        processTlv("${EMVTags.APPLICATION_CRYPTOGRAM.getHexTag()}08$ac")
    }

    private fun verifyIssuerPKCert(cert: String): Boolean {
        if (!cert.startsWith("6A02", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifyIssuerPKCert", "hash: $hash")
        val issuerPKRemainder = getICCTag(EMVTags.ISSUER_PUBLIC_KEY_REMAINDER.getHexTag()) ?: ""
        val issuerPKExponent = getICCTag(EMVTags.ISSUER_PUBLIC_KEY_EXPONENT.getHexTag()) ?: ""
        val inputData = "${cert.substring(2, cert.length - 42)}${issuerPKRemainder}${issuerPKExponent}"
        return getHash(inputData) == hash
    }

    private fun retrieveIssuerPK(): EMVPublicKey? {
        val capkIdx = getICCTag(EMVTags.CAPK_INDEX.getHexTag())
        val capkList = PreferencesUtil.getCapkData(context)
        val capk = capkList.data?.find { it.index == capkIdx } ?: return null
        Log.d("getIssuerPK", "capk: $capk")
        val issuerPKCert = getICCTag(EMVTags.ISSUER_PUBLIC_KEY_CERTIFICATE.getHexTag()) ?: return null
        Log.d("getIssuerPK", "issuerPKCert: $issuerPKCert")
        val decryptedIssuerPKCert = Encryption.doRSA(issuerPKCert, capk.exponent, capk.modulus)
        Log.d("getIssuerPK", "decryptedIssuerPKCert: $decryptedIssuerPKCert")
        if (!verifyIssuerPKCert(decryptedIssuerPKCert)) return null
        Log.d("getIssuerPK", "verifyIssuerPKCert SUCCESS")

        val length = decryptedIssuerPKCert.substring(26, 28).toInt(16) * 2
        val issuerPKRemainder = getICCTag(EMVTags.ISSUER_PUBLIC_KEY_REMAINDER.getHexTag()) ?: ""
        val issuerPKModulus = "${decryptedIssuerPKCert.substring(30, 30 + length - issuerPKRemainder.length)}${issuerPKRemainder}"
        Log.d("getIssuerPK", "modulus: $issuerPKModulus")
        val issuerPKExponent = getICCTag(EMVTags.ISSUER_PUBLIC_KEY_EXPONENT.getHexTag()) ?: return null
        return if (issuerPKModulus.length == length) EMVPublicKey(issuerPKExponent, issuerPKModulus) else null
    }

    private fun retrieveIccPK(issuerPK: EMVPublicKey): EMVPublicKey? {
        val iccPKCert = getICCTag(EMVTags.ICC_PUBLIC_KEY_CERTIFICATE.getHexTag()) ?: return null
        issuerPK.exponent ?: return null
        issuerPK.modulus ?: return null
        val decryptedIccPKCert = Encryption.doRSA(iccPKCert, issuerPK.exponent, issuerPK.modulus)
        Log.d("getIccPK", "decryptedIccPKCert: $decryptedIccPKCert")
        if (!verifyIccPKCert(decryptedIccPKCert)) return null
        Log.d("getIccPK", "verifyIccPKCert SUCCESS")

        val length = decryptedIccPKCert.substring(38, 40).toInt(16) * 2
        val iccPKRemainder = getICCTag(EMVTags.ICC_PUBLIC_KEY_REMAINDER.getHexTag()) ?: ""
        val iccPKModulus = "${decryptedIccPKCert.substring(42, 42 + length - iccPKRemainder.length)}${iccPKRemainder}"
        Log.d("getIccPK", "modulus: $iccPKModulus")
        val iccPKExponent = getICCTag(EMVTags.ICC_PUBLIC_KEY_EXPONENT.getHexTag()) ?: return null
        return if (iccPKModulus.length == length) EMVPublicKey(iccPKExponent, iccPKModulus) else null
    }

    private fun verifyIccPKCert(cert: String): Boolean {
        if (!cert.startsWith("6A04", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifyIccPKCert", "hash: $hash")
        val iccPKRemainder = getICCTag(EMVTags.ICC_PUBLIC_KEY_REMAINDER.getHexTag()) ?: ""
        val iccPKExponent = getICCTag(EMVTags.ICC_PUBLIC_KEY_EXPONENT.getHexTag()) ?: ""
        val staticAuthData = getStaticAuthData()
        Log.d("verifyIccPKCert", "staticAuthData: $staticAuthData")
        val inputData = "${cert.substring(2, cert.length - 42)}${iccPKRemainder}${iccPKExponent}$staticAuthData"
        return getHash(inputData) == hash
    }

    private fun verifySDAD(cert: String): Boolean {
        if (!Regex("^6A(05|95).*", setOf(RegexOption.IGNORE_CASE)).matches(cert)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifySDAD", "hash: $hash")
        // refer to EMV Contactless Book C-3: Table C-1
        return getICCTag(EMVTags.CARD_AUTHENTICATION_RELATED_DATA.getHexTag())?.let { cardAuthData -> // indicates fDDA perform
            val sb = StringBuilder()
            sb.append(getTerminalTag(EMVTags.UNPREDICTABLE_NUMBER.getHexTag()))
            sb.append(getTerminalTag(EMVTags.AUTHORISED_AMOUNT.getHexTag()))
            sb.append(getTerminalTag(EMVTags.TRANSACTION_CURRENCY_CODE.getHexTag()))
            sb.append(cardAuthData)
            val terminalDynamicData = sb.toString()
            Log.d("verifySDAD", "terminalDynamicData: $terminalDynamicData")
            val inputData = "${cert.substring(2, cert.length - 42)}$terminalDynamicData"
            getHash(inputData) == hash
        } ?: run {// CDA
            val inputData = "${cert.substring(2, cert.length - 42)}${getTerminalTag(EMVTags.UNPREDICTABLE_NUMBER.getHexTag())}"
            getHash(inputData) == hash
        }
    }

    private fun getStaticAuthData(): String {
        var data = ""
        getICCTag(EMVTags.STATIC_DATA_AUTHENTICATION_TAG_LIST.getHexTag())?.let { tagList ->
            TlvUtil.readTagList(tagList).forEach { tag ->
                data += getICCTag(tag) ?: getTerminalTag(tag) ?: ""
            }
        }
        return "${getOdaData()}$data"
    }

    private fun getHash(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val message = plaintext.hexToByteArray()
        val cipher = md.digest(message).toHexString().uppercase()
        Log.d("getHash", "plaintext: $plaintext -> cipher: $cipher")
        return cipher
    }

    private fun staticDataAuthentication(): Boolean {
        val issuerPK = retrieveIssuerPK() ?: run {
            Log.d("_SDA", "Retrieve Issuer Public Key Fail")
            return false
        }
        Log.d("_SDA", "issuerPK: ${issuerPK.exponent}, ${issuerPK.modulus}")
        val ssad = getICCTag(EMVTags.SIGNED_STATIC_APPLICATION_DATA.getHexTag()) ?: return false
        issuerPK.exponent ?: return false
        issuerPK.modulus ?: return false
        val decryptedSSAD = Encryption.doRSA(ssad, issuerPK.exponent, issuerPK.modulus)
        Log.d("_SDA", "decryptedSSAD: $decryptedSSAD")
        if (!verifySSAD(decryptedSSAD)) return false
        Log.d("_SDA", "verifySDAD SUCCESS")
        return true
    }

    private fun verifySSAD(cert: String): Boolean {
        if (!cert.startsWith("6A03", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifySSAD", "hash: $hash")
        val staticAuthData = getStaticAuthData()
        Log.d("verifySSAD", "staticAuthData: $staticAuthData")
        val inputData = "${cert.substring(2, cert.length - 42)}$staticAuthData"
        return getHash(inputData) == hash
    }
}