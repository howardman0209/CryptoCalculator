package com.crypto.calculator.cardReader.contactless.delegate

import android.nfc.tech.IsoDep

interface EMVCTLProcess {
    fun selectAID(isoDep: IsoDep)
    fun executeGPO(isoDep: IsoDep)
    fun readRecord(isoDep: IsoDep)
    fun getChallenge(isoDep: IsoDep): String?
    fun generateAC(isoDep: IsoDep)
    fun performODA()
    fun performCVM()
}