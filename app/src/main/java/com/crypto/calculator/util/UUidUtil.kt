package com.crypto.calculator.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure
import android.util.Log
import com.crypto.calculator.extension.toHexString
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class UUidUtil {

    companion object {

        private val hexArray = "0123456789ABCDEF".toCharArray()

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xff

                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        private fun guid(key: String): String {
            val random = SecureRandom()
            val values = ByteArray(36)
            random.nextBytes(values)
            return StringBuilder(values.contentToString())
                .append('@')
                .append(System.nanoTime().toString())
                .append('@')
                .append(UUID.randomUUID())
                .append('@')
                .append(key)
                .toString()
        }

        private fun guid(): String {
            return guid("RANDOM")
        }

        fun genUUID(): String {

            val md = MessageDigest.getInstance("SHA")

            md.update(guid().toByteArray())
            val digest = md.digest()

            return bytesToHex(digest)
        }

        fun genUuidByLength(length: Int): String {
            val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
            return (1..length).map { charset.random() }.joinToString("")
        }

        fun genHexIdByLength(length: Int): String {
            val charset = "ABCDEF0123456789"
            return (1..length).map { charset.random() }.joinToString("")
        }

        /**
         * Function to get unique app ID. Requirements are
         * 1. Unique per device (HW)
         * 2. Unique per application
         * 3. Remain constant even app install or app update
         * 4. Offline ready (preferable)
         */
        @SuppressLint("HardwareIds")
        fun getApplicationId(context: Context): String {

            val uniqueHwId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
            val uniqueSwId = context.packageName

            val srcBytes = "$uniqueHwId-$uniqueSwId".toByteArray()

            val md = MessageDigest.getInstance("SHA-224")
            md.update(srcBytes)
            val id = md.digest().toHexString()

            Log.d("UUidUtil", "app id:$id")

            return id
        }

        @SuppressLint("HardwareIds")
        fun getHardwareId(context: Context): String {
            return Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        }

    }

}