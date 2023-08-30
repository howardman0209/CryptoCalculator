package com.crypto.calculator.ui.viewModel

import android.app.Activity
import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crypto.calculator.cardReader.AndroidCardReader
import com.crypto.calculator.cardReader.BasicCardReader
import com.crypto.calculator.extension.findByKey
import com.crypto.calculator.extension.hexToBinary
import com.crypto.calculator.extension.toDataClass
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.ui.base.BaseViewModel
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.InputFilterUtil
import com.crypto.calculator.util.ODAUtil
import com.crypto.calculator.util.TlvUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmvViewModel : BaseViewModel() {
    val promptMessage: ObservableField<String> = ObservableField()
    val cardPreference: MutableLiveData<PaymentMethod> = MutableLiveData(PaymentMethod.VISA)

    val inputData1: ObservableField<String> = ObservableField()
    val inputData1Max: ObservableField<Int?> = ObservableField()
    val inputData1Label: ObservableField<String> = ObservableField()
    val inputData1Filter: MutableLiveData<List<InputFilter>> = MutableLiveData(emptyList())
    val inputData1InputType: MutableLiveData<Int> = MutableLiveData(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)

    val inputData2: ObservableField<String> = ObservableField()
    val inputData2Max: ObservableField<Int?> = ObservableField()
    val inputData2Label: ObservableField<String> = ObservableField()
    val inputData2Filter: MutableLiveData<List<InputFilter>> = MutableLiveData(emptyList())
    val inputData2InputType: MutableLiveData<Int> = MutableLiveData(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)

    var cardReader: BasicCardReader? = null
    private val gsonBeautifier: Gson = GsonBuilder().setPrettyPrinting().create()
    val currentTransactionData: HashMap<String, String> = hashMapOf()
    private var currentCAPDU: String = ""
    private var nextIsODAData: Boolean = false
    private var odaData: String = ""

    fun setInputData1Filter(maxLength: Int? = null, inputFormat: DataFormat = DataFormat.HEXADECIMAL) {
        inputData1Filter.value = emptyList()
        inputData1Max.set(maxLength)
        if (inputFormat != DataFormat.ASCII) {
            inputData1InputType.postValue(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
        } else {
            inputData1InputType.postValue(InputType.TYPE_CLASS_TEXT)
        }
        val filterList = listOfNotNull(
            maxLength?.let { InputFilterUtil.getLengthInputFilter(it) },
            when (inputFormat) {
                DataFormat.HEXADECIMAL -> InputFilterUtil.getHexInputFilter()
                DataFormat.BINARY -> InputFilterUtil.getBinInputFilter()
                DataFormat.DECIMAL -> InputFilterUtil.getDesInputFilter()
                DataFormat.ASCII -> null
            }
        )
        inputData1Filter.postValue(filterList)
    }

    fun setInputData2Filter(maxLength: Int? = null, inputFormat: DataFormat = DataFormat.HEXADECIMAL) {
        inputData2Filter.value = emptyList()
        inputData2Max.set(maxLength)
        if (inputFormat != DataFormat.ASCII) {
            inputData2InputType.postValue(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
        } else {
            inputData2InputType.postValue(InputType.TYPE_CLASS_TEXT)
        }
        val filterList = listOfNotNull(
            maxLength?.let { InputFilterUtil.getLengthInputFilter(it) },
            when (inputFormat) {
                DataFormat.HEXADECIMAL -> InputFilterUtil.getHexInputFilter()
                DataFormat.BINARY -> InputFilterUtil.getBinInputFilter()
                DataFormat.DECIMAL -> InputFilterUtil.getDesInputFilter()
                DataFormat.ASCII -> null
            }
        )
        inputData2Filter.postValue(filterList)
    }

    fun prepareCardReader(context: Context, activity: Activity) {
        cardReader = AndroidCardReader.newInstance(context, activity)

        //Use IO thread to do connect SDK such that in case SDK hang it won't hang the main thread
        viewModelScope.launch(Dispatchers.IO) {
            cardReader?.init() //init will trigger init completed and then check card will be called onwards
            cardReader?.connect()
        }
    }

    fun finishCardReader() {
        Log.d("cardReader", "finishCardReader")

        if (cardReader != null) {
            Log.d("cardReader", "disconnect and release card reader")
            //Unconditionally cancel check card and disconnect
            cardReader?.cancelCheckCard()
            cardReader?.disconnect()
            cardReader?.release()
            cardReader = null
        }
    }

    private fun saveRequiredTransactionData(jsonString: String, tagList: String) {
        val jsonObject = jsonString.toDataClass<JsonObject>()

        TlvUtil.readTagList(tagList).forEach { tag ->
            if (jsonString.contains(tag, ignoreCase = true)) {
                if (!currentTransactionData.containsKey(tag)) {
                    jsonObject.findByKey(tag).also {
                        if (it.isNotEmpty()) {
                            currentTransactionData[tag] = it.first().asString
                            Log.d("saveRequiredTransactionData", "currentTransactionData: $currentTransactionData")
                            return@forEach
                        }
                    }
                }
            }

            /**
             * Special handling for rAPDU in [Response Message Template Format 1] (tag: 80)
             */
            when (tag) {
                "82", "94" -> {
                    jsonObject.findByKey("80").also {
                        if (it.isNotEmpty()) {
                            val tlv = it.first().asString
                            when (tag) {
                                "82" -> currentTransactionData["82"] = tlv.take(4)
                                "94" -> currentTransactionData["94"] = tlv.substring(4)
                            }
                            Log.d("saveRequiredTransactionData", "currentTransactionData: $currentTransactionData")
                        }
                    }
                }

                "9F10", "9F26", "9F27", "9F36" -> {
                    jsonObject.findByKey("80").also {
                        if (it.isNotEmpty()) {
                            val tlv = it.first().asString
                            when (tag) {
                                "9F27" -> currentTransactionData["9F27"] = tlv.take(2)
                                "9F36" -> currentTransactionData["9F36"] = tlv.substring(2, 6)
                                "9F26" -> currentTransactionData["9F26"] = tlv.substring(6, 22)
                                "9F10" -> currentTransactionData["9F10"] = tlv.substring(22)
                            }
                            Log.d("saveRequiredTransactionData", "currentTransactionData: $currentTransactionData")
                        }
                    }
                }
            }
        }
    }

    private fun saveODAData(jsonString: String) {
        val jsonObject = jsonString.toDataClass<JsonObject>()
        jsonObject.findByKey("70").also {
            if (it.isNotEmpty()) {
                odaData += TlvUtil.encodeTLV(it.first().asJsonObject)
                Log.d("saveODAData", "odaData: $odaData")
            }
        }
    }

    private fun processPDOLFromGPO(cAPDU: String): String {
        val stringBuilder = StringBuilder()
        val data = cAPDU.substring(14).dropLast(2)
        currentTransactionData["9F38"]?.let { TlvUtil.readDOL(it) }?.also { pdolMap ->
            stringBuilder.append("\n*** Processing Options Data [9F38] ***")
            var cursor = 0
            pdolMap.forEach {
                stringBuilder.append("\n[${it.key}]: ${data.substring(cursor, cursor + it.value.toInt(16) * 2)}")
                cursor += it.value.toInt(16) * 2
            }
            stringBuilder.append("\n*** Processing Options Data [9F38] ***")
        }
        Log.d("processPDOLFromGPO", "Processing Options Data [9F38]: $stringBuilder")
        return stringBuilder.toString()
    }

    private fun processCDOLFromGenAC(cAPDU: String): String {
        val stringBuilder = StringBuilder()
        val data = cAPDU.substring(10).dropLast(2)
        currentTransactionData["8C"]?.let { TlvUtil.readDOL(it) }?.also { cdolMap ->
            stringBuilder.append("\n*** Card Risk Management Data [8C] ***")
            var cursor = 0
            cdolMap.forEach {
                stringBuilder.append("\n[${it.key}]: ${data.substring(cursor, cursor + it.value.toInt(16) * 2)}")
                cursor += it.value.toInt(16) * 2
            }
            stringBuilder.append("\n*** Card Risk Management Data [8C] ***")
        }
        Log.d("processCDOLFromGenAC", "Card Risk Management Data: $stringBuilder")
        return stringBuilder.toString()
    }

    fun getInspectLog(context: Context, apdu: String): String {
        val logBuilder = StringBuilder()
        when {
            apdu == "00A404000E325041592E5359532E444446303100" -> {
                logBuilder.append("Select Proximity Payment System Environment (PPSE)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
                currentCAPDU = apdu
            }

            apdu.startsWith("00A40400") -> {
                logBuilder.append("Select Application Identifier (AID)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
                val size = apdu.substringAfter("00A40400").take(2).toInt(16) * 2
                val aid = apdu.substringAfter("00A40400").substring(2, 2 + size)
                logBuilder.append("\nAID: $aid")
                currentCAPDU = apdu
            }

            apdu.startsWith("80A80000") -> {
                logBuilder.append("Get Processing Options (GPO)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
                logBuilder.append(processPDOLFromGPO(apdu))
                currentCAPDU = apdu
            }

            apdu.startsWith("00B2") -> {
                logBuilder.append("Read Record")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
                val recordNumber = apdu.substringAfter("00B2").substring(0, 2).toInt(16)
                logBuilder.append("\nRecord number: $recordNumber, ")
                currentTransactionData["94"]?.also { afl ->
                    val locators = afl.chunked(8)
                    val cla = "00"
                    val ins = "B2"
                    val le = "00"
                    locators.forEach {
                        val sfi = it.take(2)
                        val p2 = ("${sfi.hexToBinary().take(5)}000".toInt(2) + "0100".toInt(2)).toHexString()
                        val firstRecord = it.substring(2, 4).toInt(16)
                        val lastRecord = it.substring(4, 6).toInt(16)
                        var odaLabel = it.substring(7).toInt(16)
                        for (i in firstRecord..lastRecord) {
                            val p1 = String.format("%02d", i)
                            val cmd = "$cla$ins$p1$p2$le"
                            if (cmd == apdu) {
                                logBuilder.append("Short File Identifier (SFI): $sfi")
                                if (odaLabel > 0) {
                                    logBuilder.append(", ODA data: true")
                                    nextIsODAData = true
                                    odaLabel--
                                }
                            }
                        }
                    }
                }
                currentCAPDU = apdu
            }

            apdu.startsWith("80AE") -> {
                logBuilder.append("Generate Application Cryptogram (GenAC)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
                logBuilder.append(processCDOLFromGenAC(apdu))
                currentCAPDU = apdu
            }

            apdu == "0084000000" -> {
                logBuilder.append("Get Challenge")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
                currentCAPDU = apdu
            }

            else -> {
                logBuilder.append("\nrAPDU: ")
                logBuilder.append("$apdu\n")
                if (apdu.endsWith(APDU_RESPONSE_CODE_OK)) {
                    try {
                        gsonBeautifier.toJson(TlvUtil.decodeTLV(apdu)).also { jsonString ->
                            logBuilder.append(jsonString)
                            when {
                                currentCAPDU.startsWith("80A80000") -> {
                                    saveRequiredTransactionData(jsonString, "8294")
                                    if (apdu.startsWith("80")) {
                                        logBuilder.append("\n[82]: ${currentTransactionData["82"]}")
                                        logBuilder.append("\n[94]: ${currentTransactionData["94"]}")
                                    }
                                }

                                currentCAPDU.startsWith("80AE") -> {
                                    saveRequiredTransactionData(jsonString, "9F109F269F279F369F4B")

                                    currentTransactionData["9F4B"]?.also { sdad ->
                                        try {
                                            val issuerPK = ODAUtil.retrieveIssuerPK(context, currentTransactionData)
                                            val iccPK = ODAUtil.retrieveIccPK(ODAUtil.getStaticAuthData(odaData, currentTransactionData), currentTransactionData, issuerPK)
                                            val cryptogram = ODAUtil.getCryptogramFromSDAD(sdad, iccPK)
                                            Log.d("getInspectLog", "Cryptogram [9F26]: $cryptogram")
                                            logBuilder.append("\n[9F26]: $cryptogram")
                                        } catch (ex: Exception) {
                                            Log.d("getInspectLog", "Exception: $ex")
                                            logBuilder.append("\n[9F26]: ${ex.message}")
                                        }
                                    }

                                    if (apdu.startsWith("80")) {
                                        logBuilder.append("\n[9F10]: ${currentTransactionData["9F10"]}")
                                        logBuilder.append("\n[9F26]: ${currentTransactionData["9F26"]}")
                                        logBuilder.append("\n[9F27]: ${currentTransactionData["9F27"]}")
                                        logBuilder.append("\n[9F36]: ${currentTransactionData["9F36"]}")
                                    }
                                }

                                else -> {
                                    saveRequiredTransactionData(jsonString, "8C8F90929F329F389F469F479F489F4A")

                                    if (nextIsODAData) {
                                        saveODAData(jsonString)
                                        nextIsODAData = false
                                    }
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        logBuilder.append("Not in ASN.1")
                    }
                } else {
                    logBuilder.append("Command not supported")
                }
                logBuilder.append("\n")
            }
        }
        return logBuilder.toString()
    }

    fun resetTransactionData() {
        currentTransactionData.clear()
        odaData = ""
    }

    fun inspectIssuerPKPlainCert(cert: String, data: HashMap<String, String>): String {
        val logBuilder = StringBuilder()
        cert.substring(0, 2).also { logBuilder.append("${if (it == "6A") "✓" else "✗"} [Data Header]: $it\n") }
        cert.substring(2, 4).also { logBuilder.append("${if (it == "02") "✓" else "✗"} [Data Format]: $it\n") }
        cert.substring(4, 12).also { logBuilder.append("- [Issuer Identifier]: $it\n") }
        cert.substring(12, 16).also { logBuilder.append("- [Certificate Expiration Date]: $it\n") }
        cert.substring(16, 22).also { logBuilder.append("- [Certificate Serial Number]: $it\n") }
        cert.substring(22, 24).also { logBuilder.append("- [Hash Algorithm Indicator]: $it\n") }
        cert.substring(24, 26).also { logBuilder.append("- [Issuer Public Key Algorithm Indicator]: $it\n") }
        cert.substring(26, 28).also { logBuilder.append("- [Issuer Public Key Length]: $it\n") }
        val issuerPKRemainder = data["92"] ?: ""
        val issuerPKLength = cert.substring(26, 28).toInt(16) * 2 - issuerPKRemainder.length
        cert.substring(28, 30).also {
            logBuilder.append("${if (it == (data["9F32"]?.length?.div(2))?.toHexString()) "✓" else "✗"} [Issuer Public Key Exponent Length]: $it\n")
        }
        cert.substring(30, 30 + issuerPKLength).also { logBuilder.append("- [Issuer Public Key]: $it\n") }
        cert.substring(30 + issuerPKLength, cert.length - 42).also {
            if (it.isNotEmpty()) {
                logBuilder.append("- [Pad Pattern]: $it\n")
            } else {
                logBuilder.append("- [Issuer Public Key Remainder]: $issuerPKRemainder\n")
            }
        }
        cert.substring(cert.length - 42, cert.length - 2).also {
            logBuilder.append("${if (it == ODAUtil.getHash("${cert.substring(2, cert.length - 42)}${data["92"] ?: ""}${data["9F32"] ?: ""}")) "✓" else "✗"} [Hash Result]: $it\n")
        }
        cert.substring(cert.length - 2).also { logBuilder.append("${if (it == "BC") "✓" else "✗"} [Data Trailer]: $it") }
        return logBuilder.toString()
    }

    fun inspectIccPKPlainCert(cert: String, data: HashMap<String, String>): String {
        val logBuilder = StringBuilder()
        cert.substring(0, 2).also { logBuilder.append("${if (it == "6A") "✓" else "✗"} [Data Header]: $it\n") }
        cert.substring(2, 4).also { logBuilder.append("${if (it == "04") "✓" else "✗"} [Data Format]: $it\n") }
        cert.substring(4, 24).also { logBuilder.append("- [Issuer Identifier]: $it\n") }
        cert.substring(24, 28).also { logBuilder.append("- [Certificate Expiration Date]: $it\n") }
        cert.substring(28, 34).also { logBuilder.append("- [Certificate Serial Number]: $it\n") }
        cert.substring(34, 36).also { logBuilder.append("- [Hash Algorithm Indicator]: $it\n") }
        cert.substring(36, 38).also { logBuilder.append("- [ICC Public Key Algorithm Indicator]: $it\n") }
        cert.substring(38, 40).also { logBuilder.append("- [ICC Public Key Length]: $it\n") }
        val iccPKRemainder = data["9F48"] ?: ""
        val iccPKLength = cert.substring(38, 40).toInt(16) * 2 - iccPKRemainder.length
        cert.substring(40, 42).also {
            logBuilder.append("${if (it == (data["9F47"]?.length?.div(2))?.toHexString()) "✓" else "✗"} [ICC Public Key Exponent Length]: $it\n")
        }
        cert.substring(42, 42 + iccPKLength).also { logBuilder.append("- [ICC Public Key]: $it\n") }
        cert.substring(42 + iccPKLength, cert.length - 42).also {
            if (it.isNotEmpty()) {
                logBuilder.append("- [Pad Pattern]: $it\n")
            } else {
                logBuilder.append("- [ICC Public Key Remainder]: $iccPKRemainder\n")
            }
        }
        cert.substring(cert.length - 42, cert.length - 2).also {
            logBuilder.append("${if (it == ODAUtil.getHash("${cert.substring(2, cert.length - 42)}${data["9F48"] ?: ""}${data["9F47"] ?: ""}${data["staticData"] ?: ""}")) "✓" else "✗"} [Hash Result]: $it\n")
        }
        cert.substring(cert.length - 2).also { logBuilder.append("${if (it == "BC") "✓" else "✗"} [Data Trailer]: $it") }
        return logBuilder.toString()
    }

    fun inspectSignedDynamicApplicationData(sdad: String, data: HashMap<String, String>): String {
        val logBuilder = StringBuilder()
        val format = sdad.substring(2, 4)
        sdad.substring(0, 2).also { logBuilder.append("${if (it == "6A") "✓" else "✗"} [Data Header]: $it\n") }
        sdad.substring(2, 4).also { logBuilder.append("${if (it == "05" || it == "95") "✓" else "✗"} [Data Format]: $it\n") }
        sdad.substring(4, 6).also { logBuilder.append("- [Hash Algorithm Indicator]: $it\n") }
        sdad.substring(6, 8).also { logBuilder.append("- [ICC Dynamic Data length]: $it\n") }
        val dynamicDataLength = sdad.substring(6, 8).toInt(16) * 2
        val dynamicData = sdad.substring(8, 8 + dynamicDataLength)
        sdad.substring(8, 8 + dynamicDataLength).also { logBuilder.append("- [ICC Dynamic Data]: $it\n") }
        if (format == "05") {
            dynamicData.substring(0, 2).also { logBuilder.append("--- [ICC Dynamic Number Length]: $it\n") }
            val dynamicNumberLength = dynamicData.substring(0, 2).toInt(16) * 2
            dynamicData.substring(2, 2 + dynamicNumberLength).also { logBuilder.append("--- [ICC Dynamic Number]: $it\n") }
            dynamicData.substring(2 + dynamicNumberLength, 4 + dynamicNumberLength).also { logBuilder.append("--- [9F27]: $it\n") }
            dynamicData.substring(4 + dynamicNumberLength, 20 + dynamicNumberLength).also { logBuilder.append("--- [9F26]: $it\n") }
            dynamicData.substring(20 + dynamicNumberLength).also { logBuilder.append("--- [Transaction Data Hash Code]: $it\n") }
        }
        sdad.substring(8 + dynamicDataLength, sdad.length - 42).also { logBuilder.append("- [Pad Pattern]: $it\n") }
        sdad.substring(sdad.length - 42, sdad.length - 2).also {
            logBuilder.append("${if (it == ODAUtil.getHash("${sdad.substring(2, sdad.length - 42)}${data["dynamicData"] ?: ""}")) "✓" else "✗"} [Hash Result]: $it\n")
        }
        sdad.substring(sdad.length - 2).also { logBuilder.append("${if (it == "BC") "✓" else "✗"} [Data Trailer]: $it") }
        return logBuilder.toString()
    }
}