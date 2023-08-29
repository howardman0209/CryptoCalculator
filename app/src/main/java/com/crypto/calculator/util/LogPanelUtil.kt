package com.crypto.calculator.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.handler.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LogPanelUtil {
    private val _messageToLog = MutableLiveData<Event<String>>()
    val navigateToDetails: LiveData<Event<String>>
        get() = _messageToLog

    fun printLog(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            _messageToLog.value = Event(message)
        }
    }

    fun safeExecute(showError: Boolean = true, task: () -> String): String {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            if (showError) "Error: ${ex.message ?: ex}" else ""
        }
    }

    fun <T> safeExecute(onFail: ((ex: Exception) -> Unit)? = null, task: () -> T): T? {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            onFail?.invoke(ex)
            null
        }
    }
}