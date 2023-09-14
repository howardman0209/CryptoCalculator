package com.crypto.calculator.cardReader.emv

interface EmvKernelProvider {
    fun onTapEmvProcess(sendCommand: (cAPDU: String) -> String)
}