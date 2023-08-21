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

    open fun onStarted() {}

    open fun onCompleted() {
        nfcDelegate.onStatusChange(BasicCardReader.Companion.CardReaderStatus.SUCCESS)
    }

    open fun onCommunication(isoDep: IsoDep) {
        nfcDelegate.onStatusChange(BasicCardReader.Companion.CardReaderStatus.PROCESSING)
    }

    open fun onError(e: Exception) {
        nfcDelegate.onStatusChange(BasicCardReader.Companion.CardReaderStatus.FAIL)
    }

    override fun onTagDiscovered(p0: Tag?) {
        val isoDep = IsoDep.get(p0)

        try {
            isoDep.connect()
            onStarted()
            Log.d(classTag, "isoDep: connected - $isoDep")
            onCommunication(isoDep)
            isoDep.close()
            onCompleted()
            Log.d(classTag, "IsoDep: closed")
        } catch (e: Exception) {
            Log.d(classTag, "Exception: $e")
            onError(e)
        }
    }
}