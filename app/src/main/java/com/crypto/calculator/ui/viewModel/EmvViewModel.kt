package com.crypto.calculator.ui.viewModel

import android.text.InputFilter
import android.text.InputType
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.ui.base.BaseViewModel
import com.crypto.calculator.util.InputFilterUtil

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
}