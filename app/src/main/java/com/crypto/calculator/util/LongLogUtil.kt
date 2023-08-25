package com.crypto.calculator.util

import android.util.Log

object LongLogUtil {
    fun debug(tag: String, str: String) {
        if (str.length > 4000) {
            Log.d(tag, str.substring(0, 4000))
            debug(tag, str.substring(4000))
        } else Log.d(tag, str)
    }
}