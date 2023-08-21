package com.crypto.calculator.cardReader

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log

abstract class BasicNFCKernel(val nfcDelegate: NfcDelegate) : NfcAdapter.ReaderCallback {
    private val classTag = "BasicNFCKernel"
    companion object {
        enum class CardReaderStatus {
            READY,
            PROCESSING,
            Fail,
            SUCCESS
        }
    }


    interface NfcDelegate {
        fun onStatusChange(status: CardReaderStatus)
        fun onCardDataReceived(data: Map<String, String>)
    }

    open fun onStarted() {}

    open fun onCompleted() {
        nfcDelegate.onStatusChange(CardReaderStatus.SUCCESS)
    }

    open fun onCommunication(isoDep: IsoDep) {
        nfcDelegate.onStatusChange(CardReaderStatus.PROCESSING)
    }

    open fun onError(e: Exception) {
        nfcDelegate.onStatusChange(CardReaderStatus.Fail)
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