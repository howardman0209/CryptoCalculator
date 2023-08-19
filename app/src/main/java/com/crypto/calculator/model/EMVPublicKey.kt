package com.crypto.calculator.model

import com.crypto.calculator.extension.toHexString

data class EMVPublicKey(
    val exponent: String? = null,
    val modulus: String? = null,
)

fun EMVPublicKey.getModulusLength(): String? {
    return modulus?.length?.div(2)?.toHexString()
}

fun EMVPublicKey.getExponentLength(): String? {
    return exponent?.length?.div(2)?.toHexString()
}