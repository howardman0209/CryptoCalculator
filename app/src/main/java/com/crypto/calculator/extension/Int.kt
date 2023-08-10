package com.crypto.calculator.extension

fun Int?.orZero(): Int = this ?: 0

fun Int.toHexString(): String {
    val hex = this.toString(16).uppercase()
    return if (hex.length % 2 != 0) {
        hex.padStart(hex.length + 1, '0')
    } else hex
}