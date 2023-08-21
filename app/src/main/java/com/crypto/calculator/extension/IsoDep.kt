package com.crypto.calculator.extension

import android.nfc.tech.IsoDep
fun IsoDep.sendAPDU(cmd: String): String {
    val response = this.transceive(cmd.hexToByteArray())
    return response.toHexString().uppercase()
}