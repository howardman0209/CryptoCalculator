package com.crypto.calculator.util

import android.annotation.SuppressLint
import android.util.Log
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.keyManagement.SPKeyStore
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encryption {
    private const val tag = "Encryption"
    private const val MODE = "AES/CBC/PKCS5Padding"
    private const val IV = "1234567890ABCDEF"
    private const val skipSecretKey = true

    enum class PaddingMode {
        NONE,
        ZERO,
        PKCS7,
        FIXED_PATTERN
    }

    private fun wrap(): SecretKey? {
        return if (skipSecretKey) {
            null
        } else {
            SPKeyStore.getKeyFromStore(SPKeyStore.KeyType.EMV_DATA_KEY)
        }
    }

    fun encryptEmvSensitiveData(data: String): String {
        val key = wrap()

        return if (key != null) {
            val cipher = Cipher.getInstance(MODE)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(IV.toByteArray()))
            val values = cipher.doFinal(data.toByteArray())
            values.toHexString()
        } else {
            if (!skipSecretKey) {
                Log.e(tag, "No key in key store for encryption")
            }
            //Return original pan
            data
        }
    }

    fun decryptEmvSensitiveData(encPan: String): String {
        val key = wrap()

        return if (key != null) {
            val values = encPan.hexToByteArray()

            val cipher = Cipher.getInstance(MODE)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(IV.toByteArray()))
            String(cipher.doFinal(values))
        } else {
            if (!skipSecretKey) {
                Log.e(tag, "No key in key store for decryption")
            }
            //Return original pan
            encPan
        }
    }

    fun doPadding(input: ByteArray, mode: PaddingMode, blockSize: Int, fixedPattern: Byte? = null): ByteArray {

        if (mode == PaddingMode.NONE)
            return input

        var padLen = blockSize - input.size % blockSize

        if (padLen == 0) padLen = blockSize

        val output = ByteArray(padLen + input.size)

        input.copyInto(output)

        val paddingByte: Byte = when (mode) {
            PaddingMode.ZERO -> 0
            PaddingMode.PKCS7 -> padLen.toByte()
            PaddingMode.FIXED_PATTERN -> fixedPattern ?: 0
            else -> throw NotImplementedError()
        }

        Arrays.fill(output, input.size, output.size, paddingByte)

        return output
    }

    private fun getTDesKey(key: ByteArray): ByteArray {
        val outputKey = ByteArray(24)

        when (key.size) {
            8 -> {
                key.copyInto(outputKey)
                key.copyInto(outputKey, 8)
                key.copyInto(outputKey, 16)
            }

            16 -> {
                key.copyInto(outputKey)
                key.copyInto(outputKey, 16, 0, 8)
            }

            24 -> key.copyInto(outputKey)
            else -> throw IllegalArgumentException()
        }

        //Log.i("PaymentApp", "Key:${outputKey.joinToString(",")}")
        return outputKey
    }

    /**
     * Use TripleDES in CBC mode to encrypt input byte array
     * IV will be default as all zeros
     * Key will be constructed by incoming keys. key2 and key3 are optional
     * The input byte array should be already padded with correct blk size (8)
     */
    fun doTDESEncryptCBC(input: ByteArray, key: ByteArray): ByteArray? {
        val sk = SecretKeySpec(getTDesKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
            val iv = IvParameterSpec(ByteArray(8))
            cipher.init(Cipher.ENCRYPT_MODE, sk, iv)
            return cipher.doFinal(input)
        } catch (e: javax.crypto.NoSuchPaddingException) {
        } catch (e: java.security.NoSuchAlgorithmException) {
        } catch (e: java.security.InvalidKeyException) {
        } catch (e: javax.crypto.BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * Use TripleDES in CBC mode to decrypt input byte array
     * IV will be default as all zeros
     * Key will be constructed by incoming keys. key2 and key3 are optional
     * The input byte array should be already padded with correct blk size (8)
     */
    fun doTDESDecryptCBC(input: ByteArray, key: ByteArray): ByteArray? {
        val sk = SecretKeySpec(getTDesKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
            val iv = IvParameterSpec(ByteArray(8))
            cipher.init(Cipher.DECRYPT_MODE, sk, iv)
            return cipher.doFinal(input)
        } catch (e: javax.crypto.NoSuchPaddingException) {
        } catch (e: java.security.NoSuchAlgorithmException) {
        } catch (e: java.security.InvalidKeyException) {
        } catch (e: javax.crypto.BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * Use TripleDES in ECB mode to encrypt input byte array
     * Key will be constructed by incoming keys. key2 and key3 are optional
     * The input byte array should be already padded with correct blk size (8)
     */
    @SuppressLint("GetInstance")
    fun doTDESEncryptECB(input: ByteArray, key: ByteArray): ByteArray? {
        val sk = SecretKeySpec(getTDesKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            return cipher.doFinal(input)
        } catch (e: javax.crypto.NoSuchPaddingException) {
        } catch (e: java.security.NoSuchAlgorithmException) {
        } catch (e: java.security.InvalidKeyException) {
        } catch (e: javax.crypto.BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    @SuppressLint("GetInstance")
    fun doTDESEncryptECB(input: String, key: String): String? {
        return try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            val sk = SecretKeySpec(getTDesKey(key.hexToByteArray()), "DESede")
            cipher.init(Cipher.ENCRYPT_MODE, sk, SecureRandom())
            cipher.doFinal(input.hexToByteArray()).toHexString().uppercase()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Use TripleDES in ECB mode to decrypt input byte array
     * Key will be constructed by incoming keys. key2 and key3 are optional
     * The input byte array should be already padded with correct blk size (8)
     */
    @SuppressLint("GetInstance")
    fun doTDESDecryptECB(input: ByteArray, key: ByteArray): ByteArray? {
        val sk = SecretKeySpec(getTDesKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            return cipher.doFinal(input)
        } catch (e: javax.crypto.NoSuchPaddingException) {
        } catch (e: java.security.NoSuchAlgorithmException) {
        } catch (e: java.security.InvalidKeyException) {
        } catch (e: javax.crypto.BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * Use Single DES to decrypt input byte array
     * The input byte array should be already padded with correct blk size (8)
     */
    @SuppressLint("GetInstance")
    fun doDesDecryptECB(input: ByteArray, key: ByteArray): ByteArray? {
        val sk = SecretKeySpec(key, "DES")
        try {
            val cipher = Cipher.getInstance("DES/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            return cipher.doFinal(input)
        } catch (e: javax.crypto.NoSuchPaddingException) {
        } catch (e: java.security.NoSuchAlgorithmException) {
        } catch (e: java.security.InvalidKeyException) {
        } catch (e: javax.crypto.BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }


    /**
     * Encrypt Session key by input RSA public key
     */
    fun encryptSessionKeyByPublicRSA(sessionKey: ByteArray?, exponent: ByteArray, modulus: ByteArray): ByteArray {

        val factory = KeyFactory.getInstance("RSA")
        val keyInt = BigInteger(modulus.toHexString(), 16)
        val exponentInt = BigInteger(exponent.toHexString(), 16)
        val keySpeck = RSAPublicKeySpec(keyInt, exponentInt)
        val pubkey = factory.generatePublic(keySpeck)
        val encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, pubkey)

        val eRand = encryptCipher.doFinal(sessionKey)
        Log.d(tag, "ERandom:" + eRand.toHexString())
        Log.d(tag, "Random:" + sessionKey?.toHexString())

        return eRand
    }


    /**
     * Generate Key check value by using TDES
     *
     * KCV usually construct by encrypt 8 bytes of zero and use the first 4 bytes
     */
    fun generateKeyCheckValue(key: ByteArray?): ByteArray? {
        if (key == null) return null

        return doTDESEncryptECB(
            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            key
        )?.copyOf(4)
    }

    fun calculateMAC(key: String, data: String, macAlgo: String = "ISO9797ALG3MAC", cipherAlgo: String = "DESede"): String {
        return try {
            val mac = Mac.getInstance(macAlgo, BouncyCastleProvider())
            val sk = SecretKeySpec(getTDesKey(key.hexToByteArray()), cipherAlgo)
            mac.init(sk)
            mac.doFinal(data.hexToByteArray()).toHexString()
        } catch (e: Exception) {
            Log.e(tag, "Exception: $e")
            throw e
        }
    }

    fun doDES(key: String, data: String, mode: String, operation: Int): String {
        return try {
            val sk = SecretKeySpec(getTDesKey(key.hexToByteArray()), "DESede")
            val cipher = Cipher.getInstance("DESede/$mode/NoPadding")
            cipher.init(operation, sk)
            cipher.doFinal(data.hexToByteArray()).toHexString()
        } catch (e: Exception) {
            Log.e(tag, "Exception: $e")
            throw e
        }
    }

    fun doRSA(data: String, exponent: String, modulus: String): String {
        return try {
            val e = exponent.toBigInteger(16)
            val n = modulus.toBigInteger(16)
            data.toBigInteger(16).modPow(e, n).toHexString()
        } catch (e: Exception) {
            Log.e(tag, "Exception: $e")
            throw e
        }
    }
}