package com.crypto.calculator.util

import android.content.Context
import android.util.Log
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.EMVPublicKey
import java.security.MessageDigest

object ODAUtil {
    fun getHash(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val message = plaintext.hexToByteArray()
        val cipher = md.digest(message).toHexString().uppercase()
        Log.d("ODAUtil", "plaintext: $plaintext -> cipher: $cipher")
        return cipher
    }

    fun getStaticAuthData(staticData: String, dataMap: HashMap<String, String>): String {
        val data = StringBuilder()
        dataMap["9F4A"]?.let { tagList ->
            TlvUtil.readTagList(tagList).forEach { tag ->
                data.append(dataMap[tag] ?: "")
            }
        }
        return "$staticData$data"
    }

    //    required data [8F, 90, 92, 9F32]
    fun retrieveIssuerPK(context: Context, data: HashMap<String, String>): EMVPublicKey? {
        val capkIdx = data["8F"]
        val capkList = PreferencesUtil.getCapkData(context)
        val capk = capkList.data?.find { it.index == capkIdx } ?: return null
        Log.d("ODAUtil", "capk: $capk")
        val issuerPKCert = data["90"] ?: return null
        Log.d("ODAUtil", "issuerPKCert: $issuerPKCert")
        val decryptedIssuerPKCert = Encryption.doRSA(issuerPKCert, capk.exponent, capk.modulus)
        Log.d("ODAUtil", "decryptedIssuerPKCert: $decryptedIssuerPKCert")
        verifyIssuerPKCert(decryptedIssuerPKCert, data).also {
            Log.d("ODAUtil", "verifyIssuerPKCert SUCCESS: $it")
            if (!it) {
                throw Exception("Issuer Public Key Cert verification fail")
            }
        }

        val length = decryptedIssuerPKCert.substring(26, 28).toInt(16) * 2
        val issuerPKRemainder = data["92"] ?: ""
        val issuerPKModulus = "${decryptedIssuerPKCert.substring(30, 30 + length - issuerPKRemainder.length)}${issuerPKRemainder}"
        Log.d("ODAUtil", "modulus: $issuerPKModulus")
        val issuerPKExponent = data["9F32"] ?: return null
        return if (issuerPKModulus.length == length) EMVPublicKey(issuerPKExponent, issuerPKModulus) else null
    }

    fun verifyIssuerPKCert(cert: String, data: HashMap<String, String>): Boolean {
        if (!cert.startsWith("6A02", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("ODAUtil", "hash: $hash")
        val issuerPKRemainder = data["92"] ?: ""
        val issuerPKExponent = data["9F32"] ?: ""
        val inputData = "${cert.substring(2, cert.length - 42)}${issuerPKRemainder}${issuerPKExponent}"
        return getHash(inputData) == hash
    }

    //    required data [8F, 90, 92, 9F32, 9F46, 9F47, 9F48, 9F4A (82)]
    fun retrieveIccPK(staticData: String, data: HashMap<String, String>, issuerPK: EMVPublicKey?): EMVPublicKey? {
        issuerPK ?: throw Exception("Invalid input data [Issuer Public Key]")
        val iccPKCert = data["9F46"] ?: return null
        issuerPK.exponent ?: return null
        issuerPK.modulus ?: return null
        val decryptedIccPKCert = Encryption.doRSA(iccPKCert, issuerPK.exponent, issuerPK.modulus)
        Log.d("ODAUtil", "decryptedIccPKCert: $decryptedIccPKCert")
        verifyIccPKCert(decryptedIccPKCert, staticData, data).also {
            Log.d("ODAUtil", "verifyIccPKCert SUCCESS: $it")
            if (!it) {
                throw Exception("ICC Public Key Cert verification fail")
            }
        }

        val length = decryptedIccPKCert.substring(38, 40).toInt(16) * 2
        val iccPKRemainder = data["9F48"] ?: ""
        val iccPKModulus = "${decryptedIccPKCert.substring(42, 42 + length - iccPKRemainder.length)}${iccPKRemainder}"
        Log.d("ODAUtil", "modulus: $iccPKModulus")
        val iccPKExponent = data["9F47"] ?: return null
        return if (iccPKModulus.length == length) EMVPublicKey(iccPKExponent, iccPKModulus) else null
    }

    fun verifyIccPKCert(cert: String, staticData: String, data: HashMap<String, String>): Boolean {
        if (!cert.startsWith("6A04", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("ODAUtil", "hash: $hash")
        val iccPKRemainder = data["9F48"] ?: ""
        val iccPKExponent = data["9F47"] ?: ""
        val inputData = "${cert.substring(2, cert.length - 42)}${iccPKRemainder}${iccPKExponent}$staticData"
        return getHash(inputData) == hash
    }

    fun verifySDAD(cert: String, dynamicData: String?): Boolean {
        if (!Regex("^6A(05|95).*", setOf(RegexOption.IGNORE_CASE)).matches(cert)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifySDAD", "hash: $hash")
        return dynamicData?.let {
            val inputData = "${cert.substring(2, cert.length - 42)}$it"
            getHash(inputData) == hash
        } ?: false
    }

    fun verifySSAD(cert: String, staticData: String, data: HashMap<String, String>): Boolean {
        if (!cert.startsWith("6A03", ignoreCase = true)) return false
        if (!cert.endsWith("BC", ignoreCase = true)) return false
        val hash = cert.substring(cert.length - 42, cert.length - 2)
        Log.d("verifySSAD", "hash: $hash")
        val staticAuthData = getStaticAuthData(staticData, data)
        Log.d("verifySSAD", "staticAuthData: $staticAuthData")
        val inputData = "${cert.substring(2, cert.length - 42)}$staticAuthData"
        return getHash(inputData) == hash
    }

    fun getCryptogramFromSDAD(sdad: String, iccPK: EMVPublicKey?): String? {
        iccPK?.exponent ?: return null
        iccPK.modulus ?: return null
        val decryptedSDAD = Encryption.doRSA(sdad, iccPK.exponent, iccPK.modulus)
        val length = decryptedSDAD.substring(6, 8).toInt(16) * 2
        val iccDynamicData = decryptedSDAD.substring(8, 8 + length)
        val iccDynamicNumberLength = iccDynamicData.substring(0, 2).toInt(16) * 2
        val iccDynamicNumber = iccDynamicData.substring(2, 2 + iccDynamicNumberLength)
        Log.d("getCryptogramFromSDAD", "iccDynamicNumber: $iccDynamicNumber")
        val cid = iccDynamicData.substring(2 + iccDynamicNumberLength, 4 + iccDynamicNumberLength)
        Log.d("getCryptogramFromSDAD", "cid: $cid")
        val ac = iccDynamicData.substring(4 + iccDynamicNumberLength, 20 + iccDynamicNumberLength)
        val txnDataHashCode = iccDynamicData.substring(iccDynamicData.length - 40, iccDynamicData.length)
        Log.d("getCryptogramFromSDAD", "txnDataHashCode: $txnDataHashCode")
        // add cryptogram to tlv
        return ac
    }
}