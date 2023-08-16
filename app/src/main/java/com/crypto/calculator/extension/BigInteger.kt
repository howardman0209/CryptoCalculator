package com.crypto.calculator.extension

import java.math.BigInteger

fun BigInteger.toHexString(): String {
    val hex = this.toString(16).uppercase()
    return if (hex.length % 2 != 0) {
        hex.padStart(hex.length + 1, '0')
    } else hex
}