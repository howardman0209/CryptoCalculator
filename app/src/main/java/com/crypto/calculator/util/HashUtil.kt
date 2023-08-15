package com.crypto.calculator.util

import android.util.Log
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import java.security.MessageDigest

object HashUtil {
    fun getHexHash(plaintext: String, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val message = plaintext.hexToByteArray()
        val cipher = md.digest(message).toHexString().uppercase()
        Log.d("getHexHash", "plaintext: $plaintext -> cipher: $cipher")
        return cipher
    }
}