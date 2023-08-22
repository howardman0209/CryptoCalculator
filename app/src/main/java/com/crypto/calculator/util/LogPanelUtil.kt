package com.crypto.calculator.util

object LogPanelUtil {
    fun safeExecute(task: () -> String): String {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            "Error: ${ex.message ?: ex}"
        }
    }
}