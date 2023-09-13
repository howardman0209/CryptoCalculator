package com.crypto.calculator.cardReader

import com.crypto.calculator.cardReader.model.CardReaderStatus

interface CardReaderDelegate {
    fun onStatusChange(status: CardReaderStatus)
    fun onCardDataReceived(data: Map<String, String>)
}