package com.crypto.calculator.extension

fun ByteArray.toHexString() = joinToString("") { String.format("%02x", it) }

fun ByteArray.adjustDESParity(): ByteArray {
    this.forEachIndexed { i, byte ->
        val b = byte.toInt()
        this[i] = (b and 0xfe or (b shr 1 xor (b shr 2) xor (b shr 3) xor (b shr 4) xor (b shr 5) 	xor (b shr 6) xor (b shr 7) xor 0x01 and 0x01)).toByte()
    }
    return this
}
