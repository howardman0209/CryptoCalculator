package com.crypto.calculator.cardReader

import android.content.Context
import com.crypto.calculator.model.EmvConfig
import java.util.*

/*
  Mother class for card reader implementation
 */
abstract class BasicCardReader(val context: Context) {
    abstract fun init()
    abstract fun release()
    abstract fun disconnect()
    abstract fun connect()
    abstract fun initSetting()
    abstract fun startEMV(amount: String?, amountCashback: String?, emvConfig: EmvConfig)

    abstract fun cancelCheckCard()
    abstract fun sendOnlineReply(replyTLV: String?)
    abstract fun pollCardRemove()

    //For device support PIN only
    open fun sendPinEntryResult(pin: String?) {
    }

    //For device support PIN only
    open fun cancelPinEntryResult() {
    }

    //For device support PIN only
    open fun byPassPinEntryResult() {
    }

    //For device support CAPK calls
    open fun getCAPKList() {
    }

    open fun getCAPKDetail(location: String) {
    }

    open fun findCapkLocation(rid: String, index: String) {
    }

    open fun sendConfirmation(isConfirm: Boolean) {
    }

    open fun setAmount(amount: String, currency: Currency, cashbackAmount: String? = null) {
    }

    open fun abortSetAmount() {
    }

    open fun handleSelectedCard(aid: Int?) {
    }
}