package com.crypto.calculator.cardReader

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log

abstract class BasicNFCKernel(val nfcDelegate: NfcDelegate) : NfcAdapter.ReaderCallback {
    private val classTag = "BasicNFCKernel"

    interface NfcDelegate {
        fun onStatusChange(status: BasicCardReader.Companion.CardReaderStatus)
        fun onCardDataReceived(data: Map<String, String>)
    }

    abstract fun onStarted()

    abstract fun onCompleted()

    abstract fun onCommunication(isoDep: IsoDep)

    abstract fun postCommunication()

    abstract fun onError(e: Exception)

    override fun onTagDiscovered(p0: Tag?) {
        val isoDep = IsoDep.get(p0)

        try {
            isoDep.connect()
            onStarted()
            Log.d(classTag, "isoDep: connected - $isoDep")
            onCommunication(isoDep)
            isoDep.close()
            postCommunication()
            onCompleted()
            Log.d(classTag, "IsoDep: closed")
        } catch (e: Exception) {
            Log.d(classTag, "Exception: $e")
            onError(e)
        }
    }
}