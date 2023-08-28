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

    private fun saveRequiredTransactionData(jsonString: String, tag: String) {
        val jsonObject = jsonString.toDataClass<JsonObject>()

        if (jsonString.contains(tag, ignoreCase = true)) {
            if (!currentTransactionData.containsKey(tag)) {
                jsonObject.findByKey(tag).also {
                    if (it.isNotEmpty()) {
                        currentTransactionData[tag] = it.first().asString
                        Log.d("saveRequiredTransactionData", "currentTransactionData: $currentTransactionData")
                        return
                    }
                }
            }
        }

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

    fun getInspectLog(apdu: String): String {
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
                                    saveRequiredTransactionData(jsonString, "82")
                                    saveRequiredTransactionData(jsonString, "94")
                                    if (apdu.startsWith("80")) {
                                        logBuilder.append("\n[82]: ${currentTransactionData["82"]}")
                                        logBuilder.append("\n[94]: ${currentTransactionData["94"]}")
                                    }
                                }

                                currentCAPDU.startsWith("80AE") -> {
                                    saveRequiredTransactionData(jsonString, "9F10")
                                    saveRequiredTransactionData(jsonString, "9F26")
                                    saveRequiredTransactionData(jsonString, "9F27")
                                    saveRequiredTransactionData(jsonString, "9F36")
                                    if (apdu.startsWith("80")) {
                                        logBuilder.append("\n[9F10]: ${currentTransactionData["9F10"]}")
                                        logBuilder.append("\n[9F26]: ${currentTransactionData["9F26"]}")
                                        logBuilder.append("\n[9F27]: ${currentTransactionData["9F27"]}")
                                        logBuilder.append("\n[9F36]: ${currentTransactionData["9F36"]}")
                                    }
                                }

                                else -> {
                                    saveRequiredTransactionData(jsonString, "9F38")
                                    saveRequiredTransactionData(jsonString, "8C")
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
}