package com.crypto.calculator.ui.viewModel

import android.text.InputFilter
import android.text.InputType
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.extension.adjustDESParity
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.removePadding
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.ui.base.BaseViewModel
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.InputFilterUtil
import javax.crypto.Cipher

class InputViewModel:BaseViewModel() {
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

    val inputData3: ObservableField<String> = ObservableField()
    val inputData3Max: ObservableField<Int?> = ObservableField()
    val inputData3Label: ObservableField<String> = ObservableField()
    val inputData3Filter: MutableLiveData<List<InputFilter>> = MutableLiveData(emptyList())
    val inputData3InputType: MutableLiveData<Int> = MutableLiveData(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)

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

    fun setInputData3Filter(maxLength: Int? = null, inputFormat: DataFormat = DataFormat.HEXADECIMAL) {
        inputData3Filter.value = emptyList()
        inputData3Max.set(maxLength)
        if (inputFormat != DataFormat.ASCII) {
            inputData3InputType.postValue(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
        } else {
            inputData3InputType.postValue(InputType.TYPE_CLASS_TEXT)
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
        inputData3Filter.postValue(filterList)
    }

    fun desEncrypt(data: String, key: String, mode: String, padding: PaddingMethod): String {
        val plaintext = data.applyPadding(padding)
        return Encryption.doDES(key, plaintext, mode, Cipher.ENCRYPT_MODE).uppercase()
    }

    fun desDecrypt(data: String, key: String, mode: String, padding: PaddingMethod): String {
        return Encryption.doDES(key, data, mode, Cipher.DECRYPT_MODE).removePadding(padding).uppercase()
    }

    fun macCompute(data: String, key: String, padding: PaddingMethod): String {
        val plaintext = data.applyPadding(padding)
        return Encryption.calculateMAC(key, plaintext).uppercase()
    }

    fun fixDESKeyParity(key: String): String {
        return key.hexToByteArray().adjustDESParity().toHexString().uppercase()
    }

    fun rsaCompute(data: String, exponent: String, modulus: String): String {
        return Encryption.doRSA(data, exponent, modulus).uppercase()
    }
}