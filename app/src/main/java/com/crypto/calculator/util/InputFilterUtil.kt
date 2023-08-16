package com.crypto.calculator.util

import android.text.InputFilter
import android.util.Log

object InputFilterUtil {
    fun getLengthInputFilter(max: Int) = InputFilter.LengthFilter(max)
    fun getHexInputFilter(): InputFilter {
        return InputFilter { source, start, end, _, _, _ ->
            Log.d("getHexInputFilter", "source: $source, start: $start, end: $end")
            val sb = StringBuilder()
            val hexReg = Regex("(\\d)|([A-F])")
            if (hexReg.matches(source)) {
                sb.append(source)
            }
            sb.toString()
        }
    }

    fun getBinInputFilter(): InputFilter {
        return InputFilter { source, start, end, _, _, _ ->
            Log.d("getBinInputFilter", "source: $source, start: $start, end: $end")
            val sb = StringBuilder()
            val hexReg = Regex("([0-1])")
            if (hexReg.matches(source)) {
                sb.append(source)
            }
            sb.toString()
        }
    }

    fun getDesInputFilter(): InputFilter {
        return InputFilter { source, start, end, _, _, _ ->
            Log.d("getDesInputFilter", "source: $source, start: $start, end: $end")
            val sb = StringBuilder()
            val hexReg = Regex("(\\d)")
            if (hexReg.matches(source)) {
                sb.append(source)
            }
            sb.toString()
        }
    }
}