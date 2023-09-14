package com.crypto.calculator.cardReader.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.crypto.calculator.cardReader.emv.EmvKernelProvider
import com.crypto.calculator.extension.sendAPDU

class EmvNfcAdapterCallback(private val emvKernelProvider: EmvKernelProvider) : NfcAdapter.ReaderCallback {
    override fun onTagDiscovered(p0: Tag?) {
        val isoDep = IsoDep.get(p0)

        try {
            isoDep?.connect()
            Log.d("EmvNfcAdapter", "isoDep: connected - $isoDep")
            emvKernelProvider.onTapEmvProcess { cAPDU ->
                isoDep.sendAPDU(cAPDU)
            }
            isoDep?.close()
            Log.d("EmvNfcAdapter", "IsoDep: closed")
        } catch (e: Exception) {
            Log.d("EmvNfcAdapter", "Exception: $e")
            emvKernelProvider.onError(e)
        }
    }
}