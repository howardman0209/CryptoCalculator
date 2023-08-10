package com.crypto.calculator.keyManagement

import android.annotation.SuppressLint
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object SPKeyStore {
    private const val tag = "SPKeyStore"
    @SuppressLint("SdCardPath")
    private const val storePath = "/data/data/com.spectratech.configmanager/keyStoreFile"
    private val password = charArrayOf('p', '@', 's', 's', 'w', '0', 'r', 'D')
    private const val tempkey = "0123456789ABCDEF"

    enum class KeyType {
        EMV_DATA_KEY,
        EMV_PIN_KEY
    }

    fun initKeyStore(): Boolean {
        //Init data key
        Log.d(tag, "initKeyStore")
        return if (getKeyFromStore(KeyType.EMV_DATA_KEY) == null) {
            Log.d(tag, "EMV Key not existed. Inject temp key")
            val secretKeySpec = SecretKeySpec(tempkey.toByteArray(), "AES")
            putKeyIntoStore(secretKeySpec, KeyType.EMV_DATA_KEY)
        } else
            true
    }


    /**
     * Save key into key store
     * Subject to change
     */
    fun putKeyIntoStore(key: SecretKey, keyType: KeyType): Boolean {

        Log.d(tag, "saveKeyIntoKeyStore")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        try {
            keyStore.load(null, null)
            keyStore.setKeyEntry(keyType.name, key, null, null)
            val ksout = FileOutputStream(File(storePath))
            keyStore.store(ksout, password)
            ksout.close()
            Log.d(tag, "saveKeyIntoKey done")
            return true
        } catch (ex: Exception) {
            Log.d(tag, "save key in store exception!: $ex")
        }
        return false
    }

    fun getKeyFromStore(keyType: KeyType): SecretKey? {
        val sk: SecretKey?
        try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            var fis: FileInputStream? = null
            Log.d(tag, "getKeyFromStore")
            try {
                fis = FileInputStream(File(storePath))
            } catch (ex: Exception) {
                Log.d(tag, "ex in fis $ex")
            }
            keyStore.load(fis, password)
            sk = keyStore.getKey(keyType.name, null) as SecretKey
            return sk
            //sks = SecretKeySpec(keyStore.getKey("aliasKey", passwordKS).encoded, "AES")
        } catch (e: Exception) {
            Log.d(tag, "ex: $e")
        }

        return null
    }
}