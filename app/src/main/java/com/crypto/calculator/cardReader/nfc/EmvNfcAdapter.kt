package com.crypto.calculator.cardReader.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.crypto.calculator.cardReader.CardReaderDelegate
import com.crypto.calculator.cardReader.model.CardReaderStatus

class EmvNfcAdapterCallback(private val readerDelegate: CardReaderDelegate, private val emvProcess: (isoDep: IsoDep) -> Unit) : NfcAdapter.ReaderCallback {
    override fun onTagDiscovered(p0: Tag?) {
        val isoDep = IsoDep.get(p0)

        try {
            isoDep.connect()
            readerDelegate.onStatusChange(CardReaderStatus.PROCESSING)
            Log.d("EmvNfcAdapter", "isoDep: connected - $isoDep")
            emvProcess.invoke(isoDep)
            isoDep.close()
            readerDelegate.onStatusChange(CardReaderStatus.CARD_READ_OK)
            Log.d("EmvNfcAdapter", "IsoDep: closed")
        } catch (e: Exception) {
            Log.d("EmvNfcAdapter", "Exception: $e")
            readerDelegate.onStatusChange(CardReaderStatus.FAIL)
        }
    }
}