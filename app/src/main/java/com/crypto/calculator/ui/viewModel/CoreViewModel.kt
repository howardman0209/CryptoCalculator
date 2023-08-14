package com.crypto.calculator.ui.viewModel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.model.Tools
import com.crypto.calculator.ui.base.BaseViewModel

class CoreViewModel : BaseViewModel() {
    val title: ObservableField<String> = ObservableField()
    val logMessage: MutableLiveData<String> = MutableLiveData()
    val currentTool: MutableLiveData<Tools> = MutableLiveData(Tools.TLV_PARSER)

    var inputData: ObservableField<String> = ObservableField()
    var inputDataMax: ObservableField<Int> = ObservableField()

    fun printLog(message: String) {
        logMessage.postValue(message)
    }
}