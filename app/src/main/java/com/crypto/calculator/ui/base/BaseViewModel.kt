package com.crypto.calculator.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.crypto.calculator.util.DATE_TIME_DISPLAY_PATTERN_FULL
import com.crypto.calculator.util.DATE_TIME_DISPLAY_PATTERN_SO_SHORT
import io.reactivex.disposables.Disposable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

open class BaseViewModel : ViewModel() {
    val isShowLoading: MutableLiveData<Boolean> = MutableLiveData()
    val disposableList: ArrayList<Disposable> = arrayListOf()
    val apiError = MutableLiveData<String>()

    fun showLoadingIndicator(showLoading: Boolean) {
        isShowLoading.postValue(showLoading)
    }

    fun getDisposableList(): List<Disposable> = disposableList

    fun getCurrentDateTime(isShort: Boolean = false): String? {
        val formatter = if (isShort) DateTimeFormatter.ofPattern(DATE_TIME_DISPLAY_PATTERN_SO_SHORT) else DateTimeFormatter.ofPattern(DATE_TIME_DISPLAY_PATTERN_FULL)
        return LocalDateTime.now().format(formatter)
    }

    fun getCurrentTimestamp(): Long {
        return (System.currentTimeMillis() / 1000)
    }
}