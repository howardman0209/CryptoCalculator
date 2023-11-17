package com.crypto.calculator.service.cardSimulator

import android.content.Context
import android.util.Log
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.extension.hexToByteArray
import com.crypto.calculator.extension.toHexString
import com.crypto.calculator.model.EMVPublicKey
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.model.getExponentLength
import com.crypto.calculator.model.getModulusLength
import com.crypto.calculator.service.model.ApplicationCryptogram
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.TlvUtil
import java.security.MessageDigest

abstract class BasicEMVCard(val context: Context, private val iccData: HashMap<String, String>) {
    private val terminalData: HashMap<String, String> = hashMapOf()
    private var odaData = ""
    var transactionData = ""

    open val iccDynamicNumber = ""

    open val iccPublicKey: EMVPublicKey = EMVPublicKey()
    open var iccPublicRemainder = ""
    open val iccPrivateExponent = ""
    open val iccPublicKeyCertExpiration = ""
    open val iccPublicKeyCertSerialNumber = ""

    open val issuerPublicKey: EMVPublicKey = EMVPublicKey()
    open var issuerPublicRemainder = ""
    open val issuerPrivateExponent = ""
    open val issuerPublicKeyCertExpiration = ""
    open val issuerPublicKeyCertSerialNumber = ""

    open val capk: EMVPublicKey = EMVPublicKey()
    open val capkIndex = ""
    open val caPrivateExponent = ""

    open fun processTerminalDataFromGPO(cAPDU: String) {
        val data = cAPDU.substring(14).dropLast(2)
        transactionData += data
        val pdolMap = iccData["9F38"]?.let { TlvUtil.readDOL(it) } ?: throw Exception("INVALID_ICC_DATA [9F38]")
        var cursor = 0
        pdolMap.forEach {
            terminalData[it.key] = data.substring(cursor, cursor + it.value.toInt(16) * 2)
            cursor += it.value.toInt(16) * 2
        }
        Log.d("BasicEMVCard", "processTerminalDataFromGPO - terminalData: $terminalData")
    }

    open fun processTerminalDataFromGenAC(cAPDU: String) {
        val data = cAPDU.substring(10).dropLast(2)
        transactionData += data
        val cdolMap = iccData["8C"]?.let { TlvUtil.readDOL(it) } ?: throw Exception("INVALID_ICC_DATA [8C]")
        var cursor = 0
        cdolMap.forEach {
            terminalData[it.key] = data.substring(cursor, cursor + it.value.toInt(16) * 2)
            cursor += it.value.toInt(16) * 2
        }
        Log.d("BasicEMVCard", "processTerminalDataFromGenAC - terminalData: $terminalData")
    }

    open fun processODAData(rAPDU: String) {
        TlvUtil.decodeTLV(rAPDU).let {
            odaData = TlvUtil.encodeTLV("${it["70"]}")
            Log.d("BasicEMVCard", "processODAData - odaData: $odaData")
        }
    }

    open fun getStaticAuthData(): String {
        val data = StringBuilder()
        data.append(odaData)
        iccData["9F4A"]?.let { tagList ->
            TlvUtil.readTagList(tagList).forEach { tag ->
                data.append(iccData[tag] ?: terminalData[tag] ?: "")
            }
        }
        Log.d("BasicEMVCard", "getStaticAuthData - odaData: $data")
        return data.toString()
    }

    open fun getHash(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val message = plaintext.hexToByteArray()
        val cipher = md.digest(message).toHexString().uppercase()
        Log.d("BasicEMVCard", "getHash - plaintext: $plaintext -> cipher: $cipher")
        return cipher
    }

    /**
     * Calculate Cryptogram (ARQC)
     */
    abstract fun readCryptogramVersionNumber(iad: String): Int
    abstract fun getCryptogramCalculationDOL(data: HashMap<String, String>, cvn: Int): String
    abstract fun getCryptogramCalculationPadding(cvn: Int): PaddingMethod
    abstract fun getCryptogramCalculationKey(cvn: Int, pan: String, psn: String, atc: String, un: String? = null): String

    open fun getIssuerMasterKey(): String {
        val pan = iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val cardType = EMVUtils.getPaymentMethodByPan(pan)
        return EMVUtils.getIssuerMasterKeyByPaymentMethod(context, cardType)
    }

    open fun getIccMasterKey(): String {
        val pan = iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val psn = iccData["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
        return EMVUtils.deriveICCMasterKey(getIssuerMasterKey(), pan, psn)
    }

    open fun getACSessionKey(): String {
        val pan = iccData["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val atc = iccData["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
        val un = terminalData["9F37"]
        val cardType = EMVUtils.getPaymentMethodByPan(pan)
        return EMVUtils.deriveACSessionKey(cardType, getIccMasterKey(), atc, un)
    }

    open fun calculateCryptogram(): String {
        val data = terminalData + iccData
        val iad = data["9F10"] ?: throw Exception("INVALID_ICC_DATA [9F10]")
        val pan = data["57"]?.substringBefore('D') ?: throw Exception("INVALID_ICC_DATA [57]")
        val psn = data["5F34"] ?: throw Exception("INVALID_ICC_DATA [5F34]")
        val atc = data["9F36"] ?: throw Exception("INVALID_ICC_DATA [9F36]")
        val un = data["9F37"]
        val cvn = readCryptogramVersionNumber(iad)
        val key = getCryptogramCalculationKey(cvn, pan, psn, atc, un)
        val dol = getCryptogramCalculationDOL(data as HashMap<String, String>, cvn)
        val padding = getCryptogramCalculationPadding(cvn)
        return Encryption.calculateMAC(key, dol.applyPadding(padding)).uppercase()
    }

    /**
     * Calculate certificate for ODA [90, 9F46, 9F4B...]
     */
    open fun calculateIssuerPKCert(): String? {
        if (capk.modulus == null) return null
        if (issuerPublicKey.modulus == null) return null

        val dataToHash = StringBuilder()
        dataToHash.append("02")
        dataToHash.append("${(iccData["5A"] ?: iccData["57"]?.substringBefore('D'))?.take(8)}")
        dataToHash.append(issuerPublicKeyCertExpiration)
        dataToHash.append(issuerPublicKeyCertSerialNumber)
        dataToHash.append("01")
        dataToHash.append("01")
        dataToHash.append(issuerPublicKey.getModulusLength())
        dataToHash.append(issuerPublicKey.getExponentLength())
        if (issuerPublicKey.modulus!!.length <= capk.modulus!!.length - 72) {
            dataToHash.append(issuerPublicKey.modulus!!.padEnd(capk.modulus!!.length - 72, 'B'))
        } else {
            issuerPublicRemainder = issuerPublicKey.modulus!!.substring(capk.modulus!!.length - 72)
            dataToHash.append(issuerPublicKey.modulus!!.take(capk.modulus!!.length - 72))
        }
        Log.d("BasicEMVCard", "calculateIssuerPKCert - issuerPublicRemainder: $issuerPublicRemainder")
        dataToHash.append(issuerPublicRemainder)
        dataToHash.append(issuerPublicKey.exponent)
        val hash = getHash(dataToHash.toString())

        val plainCert = StringBuilder()
        plainCert.append("6A")
        plainCert.append("02")
        plainCert.append("${(iccData["5A"] ?: iccData["57"]?.substringBefore('D'))?.take(8)}")
        plainCert.append(issuerPublicKeyCertExpiration)
        plainCert.append(issuerPublicKeyCertSerialNumber)
        plainCert.append("01")
        plainCert.append("01")
        plainCert.append(issuerPublicKey.getModulusLength())
        plainCert.append(issuerPublicKey.getExponentLength())
        if (issuerPublicKey.modulus!!.length <= capk.modulus!!.length - 72) {
            plainCert.append(issuerPublicKey.modulus!!.padEnd(capk.modulus!!.length - 72, 'B'))
        } else {
            plainCert.append(issuerPublicKey.modulus!!.take(capk.modulus!!.length - 72))
        }
        plainCert.append(hash)
        plainCert.append("BC")
        Log.d("BasicEMVCard", "calculateIssuerPKCert - plainCert: $plainCert")

        return Encryption.doRSA(plainCert.toString(), caPrivateExponent, capk.modulus!!)
    }

    open fun calculateICCPKCert(): String? {
        if (issuerPublicKey.modulus == null) return null
        if (iccPublicKey.modulus == null) return null

        val dataToHash = StringBuilder()
        dataToHash.append("04")
        dataToHash.append("${(iccData["5A"] ?: iccData["57"]?.substringBefore('D'))}FFFF")
        dataToHash.append(iccPublicKeyCertExpiration)
        dataToHash.append(iccPublicKeyCertSerialNumber)
        dataToHash.append("01")
        dataToHash.append("01")
        dataToHash.append(iccPublicKey.getModulusLength())
        dataToHash.append(iccPublicKey.getExponentLength())
        if (iccPublicKey.modulus!!.length <= issuerPublicKey.modulus!!.length - 84) {
            dataToHash.append(iccPublicKey.modulus!!.padEnd(issuerPublicKey.modulus!!.length - 84, 'B'))
        } else {
            iccPublicRemainder = iccPublicKey.modulus!!.substring(issuerPublicKey.modulus!!.length - 84)
            dataToHash.append(iccPublicKey.modulus!!.take(issuerPublicKey.modulus!!.length - 84))
        }
        Log.d("BasicEMVCard", "calculateICCPKCert - iccPublicRemainder: $iccPublicRemainder")
        dataToHash.append(iccPublicRemainder)
        dataToHash.append(iccPublicKey.exponent)
        dataToHash.append(getStaticAuthData())
        val hash = getHash(dataToHash.toString())

        val plainCert = StringBuilder()
        plainCert.append("6A")
        plainCert.append("04")
        plainCert.append("${(iccData["5A"] ?: iccData["57"]?.substringBefore('D'))}FFFF")
        plainCert.append(iccPublicKeyCertExpiration)
        plainCert.append(iccPublicKeyCertSerialNumber)
        plainCert.append("01")
        plainCert.append("01")
        plainCert.append(iccPublicKey.getModulusLength())
        plainCert.append(iccPublicKey.getExponentLength())
        if (iccPublicKey.modulus!!.length <= issuerPublicKey.modulus!!.length - 84) {
            plainCert.append(iccPublicKey.modulus!!.padEnd(issuerPublicKey.modulus!!.length - 84, 'B'))
        } else {
            plainCert.append(iccPublicKey.modulus!!.take(issuerPublicKey.modulus!!.length - 84))
        }
        plainCert.append(hash)
        plainCert.append("BC")
        Log.d("BasicEMVCard", "calculateICCPKCert - plainCert: $plainCert")

        return Encryption.doRSA(plainCert.toString(), issuerPrivateExponent, issuerPublicKey.modulus!!)
    }

    open fun calculateSDAD(type: ApplicationCryptogram.Type): String? {
        if (iccPublicKey.modulus == null) return null

        val applicationCryptogram = calculateCryptogram()
        val transactionDataHashCode = getHash(transactionData) // TDHC

        val dataToHash = StringBuilder()
        dataToHash.append("05")
        dataToHash.append("01")
        val iccDynamicDataLength = 29 + iccDynamicNumber.length / 2 + 1
        dataToHash.append(iccDynamicDataLength.toHexString()) // 26H = 38D = 01 + 08 + 01 + 08 + 20
        dataToHash.append((iccDynamicNumber.length / 2).toHexString()) // 01
        dataToHash.append(iccDynamicNumber) // 08
        dataToHash.append(ApplicationCryptogram.getCryptogramInformationData(type)) // 01
        dataToHash.append(applicationCryptogram) // 08
        dataToHash.append(transactionDataHashCode) // 20
        dataToHash.append("B".repeat((iccPublicKey.modulus!!.length / 2 - iccDynamicDataLength - 25) * 2))
        dataToHash.append(terminalData["9F37"])
        val hash = getHash(dataToHash.toString())

        val plainCert = StringBuilder()
        plainCert.append("6A")
        plainCert.append("05")
        plainCert.append("01")
        plainCert.append(iccDynamicDataLength.toHexString()) // 26H = 38D = 01 + 08 + 01 + 08 + 20
        plainCert.append((iccDynamicNumber.length / 2).toHexString()) // 01
        plainCert.append(iccDynamicNumber) // 08
        plainCert.append(ApplicationCryptogram.getCryptogramInformationData(type)) // 01
        plainCert.append(applicationCryptogram) // 08
        plainCert.append(transactionDataHashCode) // 20
        plainCert.append("B".repeat((iccPublicKey.modulus!!.length / 2 - iccDynamicDataLength - 25) * 2))
        plainCert.append(hash)
        plainCert.append("BC")
        Log.d("BasicEMVCard", "calculateSDAD - plainCert: $plainCert")

        return Encryption.doRSA(plainCert.toString(), iccPrivateExponent, iccPublicKey.modulus!!)
    }
}