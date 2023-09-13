package com.crypto.calculator.cardReader

import android.content.Context
import android.nfc.tech.IsoDep
import com.crypto.calculator.extension.sendAPDU

class AndroidEmvKernel(
    context: Context, readerDelegate: CardReaderDelegate, terminalConfig: HashMap<String, String>,
    private val isoDep: IsoDep,
) : BasicEmvKernel(context, readerDelegate) {
    init {
        saveTerminalData(terminalConfig)
    }

    override fun sendCommand(cmd: String): String {
        return isoDep.sendAPDU(cmd)
    }
}