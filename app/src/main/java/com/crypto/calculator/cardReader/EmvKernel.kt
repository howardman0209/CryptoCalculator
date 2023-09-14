package com.crypto.calculator.cardReader

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.cardReader.contactless.BasicCTLKernel
import com.crypto.calculator.cardReader.contactless.CTLKernelFactory
import com.crypto.calculator.cardReader.contactless.EMVCTLKernel0
import com.crypto.calculator.cardReader.emv.EmvKernelProvider
import com.crypto.calculator.cardReader.model.CardReaderStatus
import com.crypto.calculator.handler.Event
import com.crypto.calculator.model.EMVTags
import com.crypto.calculator.model.getHexTag
import com.crypto.calculator.util.APDU_COMMAND_2PAY_SYS_DDF01
import com.crypto.calculator.util.APDU_RESPONSE_CODE_OK
import com.crypto.calculator.util.TlvUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmvKernel(val context: Context, private val readerDelegate: CardReaderDelegate, terminalConfig: HashMap<String, String>) : EmvKernelProvider {
    private var cardData: HashMap<String, String> = hashMapOf()
    private var terminalData: HashMap<String, String> = hashMapOf()
    private var odaData = ""
    private var ctlKernel: BasicCTLKernel? = null
    lateinit var sendCommand: (cAPDU: String) -> String

    init {
        saveTerminalData(terminalConfig)
    }

    companion object {
        private val _apdu = MutableLiveData<Event<String>>()
        val apdu: LiveData<Event<String>>
            get() = _apdu
    }

    fun onStatusChange(status: CardReaderStatus) {
        Log.d("BasicEmvKernel", "onStatusChange, status: $status")
        when (status) {
            CardReaderStatus.SUCCESS -> {
                readerDelegate.onCardDataReceived(getICCData() + getTerminalData())
                clearICCData()
                clearOdaData()
                clearTerminalData()
            }

            CardReaderStatus.FAIL -> {
                clearICCData()
                clearOdaData()
            }

            else -> {}
        }
    }

//    abstract fun sendCommand(cmd: String): String

    fun communicator(cmd: String): String {
        val cAPDU = cmd.uppercase()
        val rAPDU = sendCommand(cmd).uppercase()
        CoroutineScope(Dispatchers.Main).launch {
            _apdu.value = Event(cAPDU)
            _apdu.value = Event(rAPDU)
        }
        if (rAPDU.endsWith(APDU_RESPONSE_CODE_OK)) {
            Log.i("communicator", "cAPDU ->> $cAPDU")
            Log.i("communicator", "rAPDU <<- $rAPDU")
        } else {
            Log.e("communicator", "cAPDU ->> $cAPDU")
            Log.e("communicator", "rAPDU <<- $rAPDU")
            throw Exception("Command not supported") // TODO: read sw1, sw2 to throw exception message
        }
        return rAPDU
    }

    fun processTlv(tlv: String) {
        val decodedMap = TlvUtil.parseTLV(tlv)
        Log.d("processTlv", "tag data: $decodedMap")
        val tmp = decodedMap.mapValues { it.value.first() }
        Log.d("processTlv", "tags to be save: $tmp")
        saveICCData(tmp)
    }

    fun saveICCData(data: Map<String, String>) {
        data.forEach {
            if (!TlvUtil.isTemplateTag(it.key) && !cardData.containsKey(it.key)) {
                cardData[it.key] = it.value
            }
        }
    }

    fun getICCTag(tag: String): String? {
        return cardData[tag]
    }

    private fun clearICCData() {
        cardData.clear()
    }

    fun getICCData() = cardData

    fun saveTerminalData(data: Map<String, String>) {
        terminalData += data
    }

    fun getTerminalTag(tag: String): String? {
        return terminalData[tag]
    }

    fun getTerminalData() = terminalData

    private fun clearTerminalData() {
        terminalData.clear()
    }

    fun saveOdaData(data: String) {
        odaData += data
    }

    fun getOdaData() = odaData

    private fun clearOdaData() {
        odaData = ""
    }

    fun ppse() {
        val tlv = communicator(APDU_COMMAND_2PAY_SYS_DDF01)
        val appTemplates = TlvUtil.findByTag(tlv, tag = EMVTags.APPLICATION_TEMPLATE.getHexTag())
        Log.d("ppse", "appTemplates: $appTemplates")
        val finalTlv = appTemplates?.let { appList ->
            // check if more than 1 aid return
            if (appList.size > 1) {
                Log.d("ppse", "multiple AID read from card")
                // CTL -> auto select app with higher Application Priority Indicator
                appList.minBy { TlvUtil.decodeTLV(it)[EMVTags.APPLICATION_PRIORITY_INDICATOR.getHexTag()].toString().toInt(16) }
            } else {
                Log.d("ppse", "single AID read from card")
                appList.first()
            }
        }
        Log.d("ppse", "finalTlv: $finalTlv")
        finalTlv?.let {
            processTlv(it)
        }
    }

    override fun onTapEmvProcess(sendCommand: (cAPDU: String) -> String) {
        this.sendCommand = sendCommand
        ppse()
        ctlKernel = CTLKernelFactory.create(this)?.apply {
            when (this) {
                is EMVCTLKernel0 -> Log.d("BasicEmvKernel", "Kernel 0 is selected")
            }
            onTapEmvProcess()
        }
    }

    fun postTapEmvProcess() {
        ctlKernel?.postTapEmvProcess()
        readerDelegate.onStatusChange(CardReaderStatus.SUCCESS)
    }
}