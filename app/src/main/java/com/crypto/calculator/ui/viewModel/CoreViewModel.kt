package com.crypto.calculator.ui.viewModel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.BaseViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class CoreViewModel : BaseViewModel() {
    val gsonBeautifier: Gson = GsonBuilder().setPrettyPrinting().create()
    val title: ObservableField<String> = ObservableField()
    val logMessage: MutableLiveData<String> = MutableLiveData()
    val currentTool: MutableLiveData<Tool> = MutableLiveData(Tool.TLV_PARSER)

    var inputData: ObservableField<String> = ObservableField()
    var inputDataMax: ObservableField<Int?> = ObservableField()

    var inputKey: ObservableField<String> = ObservableField()
    var inputKeyMax: ObservableField<Int?> = ObservableField()

    fun printLog(message: String) {
        logMessage.postValue(message)
    }
}