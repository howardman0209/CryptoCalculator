package com.crypto.calculator.cardReader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.util.Log
import com.crypto.calculator.cardReader.emv.EmvKernelProvider
import com.crypto.calculator.cardReader.model.CardReaderStatus
import com.crypto.calculator.cardReader.nfc.EmvNfcAdapterCallback
import com.crypto.calculator.extension.hexToAscii
import com.crypto.calculator.model.EMVTags
import com.crypto.calculator.model.EmvConfig
import com.crypto.calculator.model.getHexTag
import com.crypto.calculator.util.DATE_TIME_PATTERN_EMV_9A
import com.crypto.calculator.util.DATE_TIME_PATTERN_EMV_9F21
import com.crypto.calculator.util.TlvUtil
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AndroidCardReader(context: Context, val activity: Activity) : BasicCardReader(context), CardReaderDelegate {
    private var nfcAdapter: NfcAdapter? = null
    private var emvKernel: EmvKernelProvider? = null

    private val requiredTagList = listOf(
        EMVTags.APPLICATION_LABEL.getHexTag(),
        EMVTags.TRACK2.getHexTag(),
        EMVTags.PAN.getHexTag(),
        EMVTags.APPLICATION_EXPIRATION_DATE.getHexTag(),
        EMVTags.TRANSACTION_CURRENCY_CODE.getHexTag(),
        EMVTags.PAN_SEQUENCE_NUMBER.getHexTag(),
        EMVTags.APPLICATION_INTERCHANGE_PROFILE.getHexTag(),
        EMVTags.DEDICATED_FILE_NAME.getHexTag(),
        EMVTags.TERMINAL_VERIFICATION_RESULT.getHexTag(),
        EMVTags.TRANSACTION_DATE.getHexTag(),
        EMVTags.TRANSACTION_TYPE.getHexTag(),
        EMVTags.AUTHORISED_AMOUNT.getHexTag(),
        EMVTags.AMOUNT_OTHER.getHexTag(),
        EMVTags.APPLICATION_IDENTIFIER.getHexTag(),
        EMVTags.APPLICATION_USAGE_CONTROL.getHexTag(),
        EMVTags.APPLICATION_VERSION_NUMBER_TERMINAL.getHexTag(),
        EMVTags.ISSUER_APPLICATION_DATA.getHexTag(),
        EMVTags.TERMINAL_COUNTRY_CODE.getHexTag(),
        EMVTags.INTERFACE_DEVICE_SERIAL_NUMBER.getHexTag(),
        EMVTags.TRANSACTION_TIME.getHexTag(),
        EMVTags.APPLICATION_CRYPTOGRAM.getHexTag(),
        EMVTags.CRYPTOGRAM_INFORMATION_DATA.getHexTag(),
        EMVTags.TERMINAL_CAPABILITIES.getHexTag(),
        EMVTags.CVM_RESULTS.getHexTag(),
        EMVTags.TERMINAL_TYPE.getHexTag(),
        EMVTags.APPLICATION_TRANSACTION_COUNTER.getHexTag(),
        EMVTags.UNPREDICTABLE_NUMBER.getHexTag(),
        EMVTags.POS_ENTRY_MODE.getHexTag(),
        EMVTags.APPLICATION_CURRENCY_CODE.getHexTag(),
        EMVTags.TERMINAL_TRANSACTION_QUALIFIERS.getHexTag(),
        EMVTags.TAG_9F6D.getHexTag(),
        EMVTags.FORM_FACTOR_INDICATOR.getHexTag(),
    )

    companion object {
        fun newInstance(context: Context, activity: Activity): AndroidCardReader {
            return AndroidCardReader(context, activity)
        }
    }

    override fun init() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }

    private fun enableReader() {
        val callback = emvKernel?.let { EmvNfcAdapterCallback(it) }
        onStatusChange(CardReaderStatus.READY)

        nfcAdapter?.enableReaderMode(
            activity, callback,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    private fun disableReader() {
        if (!activity.isDestroyed) {
            nfcAdapter?.disableReaderMode(activity)
        }
    }

    override fun release() {
        nfcAdapter = null
    }

    override fun disconnect() {
        disableReader()
    }

    override fun connect() {
        Log.d("AndroidCR", "connect")
    }

    override fun initSetting() {
        Log.d("AndroidCR", "initSetting")
    }

    override fun startEMV(authorizedAmount: String?, cashbackAmount: String?, emvConfig: EmvConfig) {
        val terminalConfig = emvConfig.data
        terminalConfig[EMVTags.AUTHORISED_AMOUNT.getHexTag()] = authorizedAmount?.padStart(12, '0')
            ?: throw Exception("INVALID_AUTHORISED_AMOUNT")
        terminalConfig[EMVTags.AMOUNT_OTHER.getHexTag()] = cashbackAmount ?: "0".padStart(12, '0')
        getCurrentTime(DATE_TIME_PATTERN_EMV_9A)?.let { terminalConfig[EMVTags.TRANSACTION_DATE.getHexTag()] = it }
        getCurrentTime(DATE_TIME_PATTERN_EMV_9F21)?.let { terminalConfig[EMVTags.TRANSACTION_TIME.getHexTag()] = it }
        Log.d("AndroidCR", "startEMV - data: $terminalConfig")
        emvKernel = EmvKernel(context, this).apply {
            loadTerminalConfig(terminalConfig)
        }
        enableReader()
    }

    override fun cancelCheckCard() {
        Log.d("AndroidCR", "cancelCheckCard")
    }

    override fun sendOnlineReply(replyTLV: String?) {
        Log.d("AndroidCR", "sendOnlineReply")
        replyTLV?.also {
            val authResponseCode = TlvUtil.findByTag(it, "8A")?.first()?.hexToAscii()
            Log.d("AndroidCR", "authResponseCode: $authResponseCode")
            when (authResponseCode) {
                "00" -> Log.d("AndroidCR", "APPROVED")
                else -> Log.d("AndroidCR", "DECLINED")
            }
        }
    }

    override fun pollCardRemove() {
        Log.d("AndroidCR", "pollCardRemove")
    }

    override fun onStatusChange(status: CardReaderStatus) {
        Log.d("AndroidCR", "CardReaderStatus: $status")
        this.status.postValue(status)
        when (status) {
            CardReaderStatus.CARD_READ_OK -> {
                disableReader()
                emvKernel?.postTapEmvProcess()
            }

            else -> {}
        }
    }

    override fun onCardDataReceived(data: Map<String, String>) {
        Log.d("CardReader", "onEmvDataReceived: $data")
        val tlv = TlvUtil.encodeTLV(handleOnlineData(data))
        Log.d("CardReader", "final check: \n$tlv \n${Gson().toJson(TlvUtil.decodeTLV(tlv))}")
    }

    private fun handleOnlineData(
        data: Map<String, String>,
        withFilter: Boolean = true
    ): Map<String, String> {
        val tmp = data.toMutableMap()
        data[EMVTags.APPLICATION_IDENTIFIER_CARD.getHexTag()]?.let { tmp[EMVTags.DEDICATED_FILE_NAME.getHexTag()] = it }
        data[EMVTags.APPLICATION_IDENTIFIER_CARD.getHexTag()]?.let { tmp[EMVTags.APPLICATION_IDENTIFIER.getHexTag()] = it }
        Log.d("handleTerminalData", "all tag data: ${Gson().toJson(tmp)}")
        val filtered = tmp.filter { requiredTagList.contains(it.key) }
        Log.d("handleTerminalData", "filtered tag data: ${Gson().toJson(filtered)}")

        return if (withFilter) filtered.toSortedMap() else tmp.toSortedMap()
    }

    @SuppressLint("NewApi")
    private fun getCurrentTime(format: String): String? {
        val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
        return LocalDateTime.now().format(formatter)
    }
}