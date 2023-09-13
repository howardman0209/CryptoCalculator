package com.crypto.calculator.cardReader.contactless.delegate
interface EMVCTLProcess {
    fun selectAID()
    fun executeGPO()
    fun readRecord()
    fun getChallenge(): String?
    fun generateAC()
    fun performODA()
    fun performCVM()
}