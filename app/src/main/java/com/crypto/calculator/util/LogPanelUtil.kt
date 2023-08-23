package com.crypto.calculator.util

object LogPanelUtil {
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