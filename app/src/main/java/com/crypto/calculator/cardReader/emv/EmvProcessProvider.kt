package com.crypto.calculator.cardReader.emv

interface EmvKernelProvider {
    fun loadTerminalConfig(terminalConfig: HashMap<String, String>)
    fun onTapEmvProcess(sendCommand: (cAPDU: String) -> String)
    fun postTapEmvProcess()
    fun onError(exception: Exception)
}