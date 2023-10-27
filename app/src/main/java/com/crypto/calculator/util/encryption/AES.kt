package com.crypto.calculator.util.encryption

import android.annotation.SuppressLint
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@SuppressWarnings("all")
@SuppressLint("all")

object AES {
    enum class Padding {
        NoPadding,
        PKCS1Padding,
        PKCS5Padding,
        PKCS7Padding
    }

    /**
     * encrypt data in ECB mode
     *
     * @param data
     * @param key
     * @return
     */
    fun encrypt(data: ByteArray?, key: ByteArray, padding: Padding): ByteArray? {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = when (padding) {
                Padding.NoPadding -> Cipher.getInstance("AES/ECB/NoPadding")
                Padding.PKCS1Padding -> Cipher.getInstance("AES/ECB/PKCS1Padding")
                Padding.PKCS5Padding -> Cipher.getInstance("AES/ECB/PKCS5Padding")
                Padding.PKCS7Padding -> Cipher.getInstance("AES/ECB/PKCS7Padding")
                else -> Cipher.getInstance("AES/ECB/NoPadding")
            }
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * decrypt data in ECB mode
     *
     * @param data
     * @param key
     * @return
     */
    fun decrypt(data: ByteArray?, key: ByteArray, padding: Padding): ByteArray? {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = when (padding) {
                Padding.NoPadding -> Cipher.getInstance("AES/ECB/NoPadding")
                Padding.PKCS1Padding -> Cipher.getInstance("AES/ECB/PKCS1Padding")
                Padding.PKCS5Padding -> Cipher.getInstance("AES/ECB/PKCS5Padding")
                Padding.PKCS7Padding -> Cipher.getInstance("AES/ECB/PKCS7Padding")
                else -> Cipher.getInstance("AES/ECB/NoPadding")
            }
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            return cipher.doFinal(data)
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * encrypt data in CBC mode
     *
     * @param data
     * @param key
     * @param IV
     * @return
     */
    fun encryptCBC(data: ByteArray, key: ByteArray, IV: ByteArray? = null, padding: Padding): ByteArray? {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = when (padding) {
                Padding.NoPadding -> Cipher.getInstance("AES/CBC/NoPadding")
                Padding.PKCS1Padding -> Cipher.getInstance("AES/CBC/PKCS1Padding")
                Padding.PKCS5Padding -> Cipher.getInstance("AES/CBC/PKCS5Padding")
                Padding.PKCS7Padding -> Cipher.getInstance("AES/CBC/PKCS7Padding")
                else -> Cipher.getInstance("AES/ECB/NoPadding")
            }
            if (IV == null)
                cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            else
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(IV))
            return cipher.doFinal(data)
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * decrypt data in CBC mode
     *
     * @param data
     * @param key
     * @return
     */
    fun decryptCBC(data: ByteArray, key: ByteArray, IV: ByteArray? = null, padding: Padding): ByteArray? {
        val keySpec = SecretKeySpec(key, "AES")
        try {
            val cipher = when (padding) {
                Padding.NoPadding -> Cipher.getInstance("AES/CBC/NoPadding")
                Padding.PKCS1Padding -> Cipher.getInstance("AES/CBC/PKCS1Padding")
                Padding.PKCS5Padding -> Cipher.getInstance("AES/CBC/PKCS5Padding")
                Padding.PKCS7Padding -> Cipher.getInstance("AES/CBC/PKCS7Padding")
                else -> Cipher.getInstance("AES/ECB/NoPadding")
            }
            if (IV == null)
                cipher.init(Cipher.DECRYPT_MODE, keySpec)
            else
                cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(IV))
            return cipher.doFinal(data)
        } catch (e: Exception) {
        }
        return null
    }

    /**
     * encrypt data in ECB mode
     *
     * @param data
     * @param key
     * @return
     */
    fun encrypt(data: String, key: String, padding: Padding): String? {
        val result: String?
        val bData: ByteArray = data.hexToByteArray()
        val bKey: ByteArray = key.hexToByteArray()
        val bOutput: ByteArray? = encrypt(bData, bKey, padding)
        result = bOutput?.toHexString()
        return result
    }

    /**
     * decrypt data in ECB mode
     *
     * @param data
     * @param key
     * @return
     */
    fun decrypt(data: String, key: String, padding: Padding): String? {
        val result: String?
        val bOutput: ByteArray?
        val bData: ByteArray = data.hexToByteArray()
        val bKey: ByteArray = key.hexToByteArray()
        bOutput = decrypt(bData, bKey, padding)
        result = bOutput?.toHexString()
        return result
    }

    /**
     * encrypt data in CBC mode
     *
     * @param data
     * @param key
     * @return
     */
    fun encryptCBC(data: String, key: String, iv: String? = null, padding: Padding): String? {
        val bData: ByteArray = data.hexToByteArray()
        val bKey: ByteArray = key.hexToByteArray()
        val bOutput: ByteArray? = encryptCBC(bData, bKey, iv?.hexToByteArray(), padding)
        val result: String? = bOutput?.toHexString()
        return result
    }

    /**
     * decrypt data in CBC mode
     *
     * @param data
     * @param key
     * @return
     */
    fun decryptCBC(data: String, key: String, iv: String? = null, padding: Padding): String? {
        val bData: ByteArray = data.hexToByteArray()
        val bKey: ByteArray = key.hexToByteArray()
        val bOutput: ByteArray? = decryptCBC(bData, bKey, iv?.hexToByteArray(), padding)
        val result: String? = bOutput?.toHexString()
        return result
    }

}

