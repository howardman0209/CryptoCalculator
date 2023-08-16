package com.crypto.calculator.util

import android.text.InputFilter
import android.util.Log

object InputFilterUtil {
    fun getLengthInputFilter(max: Int) = InputFilter.LengthFilter(max)
    fun getHexInputFilter(): InputFilter {
        return InputFilter { source, start, end, _, _, _ ->
            val sb = StringBuilder()
            val hexReg = Regex("(\\d)|([A-F])")
            Log.d("getHexInputFilter", "source: $source, start: $start, end: $end")
            for (i in start until end) {
                if (hexReg.matches(source[i].toString())) {
                    sb.append(source[i])
                }
            }
            sb.toString()
        }
    }

    fun getBinInputFilter(): InputFilter {
        return InputFilter { source, start, end, _, _, _ ->
            val sb = StringBuilder()
            val binReg = Regex("([0-1])")
            Log.d("getBinInputFilter", "source: $source, start: $start, end: $end")
            for (i in start until end) {
                if (binReg.matches(source[i].toString())) {
                    sb.append(source[i])
                }
            }
            sb.toString()
        }
    }

    fun getDesInputFilter(): InputFilter {
        return InputFilter { source, start, end, _, _, _ ->
            val sb = StringBuilder()
            val decReg = Regex("(\\d)")
            Log.d("getDesInputFilter", "source: $source, start: $start, end: $end")
            for (i in start until end) {
                if (decReg.matches(source[i].toString())) {
                    sb.append(source[i])
                }
            }
            sb.toString()
        }
    }
}