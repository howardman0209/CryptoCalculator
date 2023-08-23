package com.crypto.calculator.service.cardSimulator

import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.model.EMVPublicKey
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.util.Encryption

abstract class BasicEMVCard(private val iccData: HashMap<String, String>) {
    open val terminalData: HashMap<String, String> = hashMapOf()
    open var odaData = ""
    open var transactionData = ""
    open val iccDynamicNumber = ""
    open val iccPublicKey: EMVPublicKey = EMVPublicKey()
    open var iccPublicRemainder = ""
    open val iccPrivateExponent = ""
    open val issuerPublicKey: EMVPublicKey = EMVPublicKey()
    open var issuerPublicRemainder = ""
    open val issuerPrivateExponent = ""
    open val capk: EMVPublicKey = EMVPublicKey()
    open val capkIndex = ""
    open val caPrivateExponent = ""

    //TODO: Factorize code
    abstract fun readCryptogramVersionNumber(iad: String): Int
    abstract fun getCryptogramCalculationDOL(data: HashMap<String, String>, cvn: Int): String
    abstract fun getCryptogramCalculationPadding(cvn: Int): PaddingMethod
    abstract fun getCryptogramCalculationKey(cvn: Int, pan: String, psn: String, atc: String, un: String? = null): String
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
}