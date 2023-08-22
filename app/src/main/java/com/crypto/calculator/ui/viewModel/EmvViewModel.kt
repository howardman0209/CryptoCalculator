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
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.ui.base.BaseViewModel
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.InputFilterUtil
import com.crypto.calculator.util.LogPanelUtil
import com.crypto.calculator.util.TlvUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    val gsonBeautifier: Gson = GsonBuilder().setPrettyPrinting().create()

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

    fun getInspectLog(apdu: String): String {
        val logBuilder = StringBuilder()
        when {
            apdu == "00A404000E325041592E5359532E444446303100" -> {
                logBuilder.append("Select Proximity Payment System Environment (PPSE)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
            }

            apdu.startsWith("00A40400") -> {
                logBuilder.append("Select Application Identifier (AID)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
            }

            apdu.startsWith("80A80000") -> {
                logBuilder.append("Get Processing Options (GPO)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
            }

            apdu.startsWith("00B2") -> {
                logBuilder.append("Read Record")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
            }

            apdu.startsWith("80AE") -> {
                logBuilder.append("Generate Application Cryptogram (GenAC)")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
            }

            apdu == "0084000000" -> {
                logBuilder.append("Get Challenge")
                logBuilder.append("\ncAPDU: ")
                logBuilder.append(apdu)
            }

            else -> {
                logBuilder.append("\nrAPDU: ")
                logBuilder.append("$apdu\n")
                if (apdu.endsWith(APDU_RESPONSE_CODE_OK)) {
                    LogPanelUtil.safeExecute(false) { gsonBeautifier.toJson(TlvUtil.decodeTLV(apdu)) }.also { jsonString ->
                        if (jsonString.isNotEmpty()) {
                            logBuilder.append(jsonString)
                        } else {
                            logBuilder.append("Not in ASN.1")
                        }
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