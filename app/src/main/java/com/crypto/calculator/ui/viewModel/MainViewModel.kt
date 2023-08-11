package com.crypto.calculator.ui.viewModel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.ui.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    val title: ObservableField<String> = ObservableField()
    val logMessage: MutableLiveData<String> = MutableLiveData()

    fun printLog(message: String) {
        logMessage.postValue(message)
    }
}