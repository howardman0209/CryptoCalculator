package com.crypto.calculator.util

import androidx.lifecycle.MutableLiveData

object LogPanelUtil {
    val logMessage: MutableLiveData<String> = MutableLiveData()
    fun printLog(message: String) {
        logMessage.postValue(message)
    }
    fun safeExecute(showError: Boolean = true, task: () -> String): String {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            if (showError) "Error: ${ex.message ?: ex}" else ""
        }
    }

    fun <T> safeExecute(onFail: (() -> Unit)? = null, task: () -> T): T? {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            onFail?.invoke()
            null
        }
    }
}