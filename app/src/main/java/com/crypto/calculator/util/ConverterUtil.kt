package com.crypto.calculator.util

import com.crypto.calculator.extension.asciiToHex
import com.crypto.calculator.extension.hexToAscii
import com.crypto.calculator.extension.hexToBinary
import com.crypto.calculator.model.DataFormat

object ConverterUtil {
    fun convertString(data: String, fromFormat: DataFormat, toFormat: DataFormat): String {
        val hexData = when (fromFormat) {
            DataFormat.HEXADECIMAL -> data
            DataFormat.BINARY -> data.toLong(2).toString(16)
            DataFormat.DECIMAL -> data.toLong(10).toString(16)
            DataFormat.ASCII -> data.asciiToHex()
        }

        return when (toFormat) {
            DataFormat.HEXADECIMAL -> hexData.uppercase()
            DataFormat.BINARY -> hexData.hexToBinary()
            DataFormat.DECIMAL -> hexData.toLong(16).toString()
            DataFormat.ASCII -> hexData.hexToAscii()
        }
    }
}