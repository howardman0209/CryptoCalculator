package com.crypto.calculator.util

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LogPanelUtil {
    val logMessage: MutableLiveData<String> = MutableLiveData()
    fun printLog(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logMessage.postValue(message)
        }
    }

    fun safeExecute(showError: Boolean = true, task: () -> String): String {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            if (showError) "Error: ${ex.message ?: ex}" else ""
        }
    }

    fun <T> safeExecute(onFail: ((ex:Exception) -> Unit)? = null, task: () -> T): T? {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            onFail?.invoke(ex)
            null
        }
    }
}