package com.crypto.calculator.cardReader

import android.content.Context
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.cardReader.contactless.BasicCTLKernel
import com.crypto.calculator.cardReader.contactless.CTLKernelFactory
import com.crypto.calculator.cardReader.contactless.EMVCTLKernel0
import com.crypto.calculator.handler.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EMVCore(val context: Context, nfcDelegate: NfcDelegate) : BasicEMVKernel(nfcDelegate) {
    private var ctlKernel: BasicCTLKernel? = null

    companion object {
        private val _apdu = MutableLiveData<Event<String>>()
        val apdu: LiveData<Event<String>>
            get() = _apdu
    }

    override fun onCommunication(isoDep: IsoDep) {
        super.onCommunication(isoDep)
        ppse(isoDep)
        ctlKernel = CTLKernelFactory.create(getICCData(), this)?.apply {
            when (this) {
                is EMVCTLKernel0 -> Log.d("EMVCore", "Kernel 0 is selected")
            }
            onCommunication(isoDep)
        }
    }

    override fun postCommunication() {
        super.postCommunication()
        ctlKernel?.postCommunication()
    }

    override fun communicator(isoDep: IsoDep, cmd: String): String {
        val cAPDU = cmd.uppercase()
        val rAPDU = super.communicator(isoDep, cmd)
        CoroutineScope(Dispatchers.Main).launch {
            _apdu.value = Event(cAPDU)
            _apdu.value = Event(rAPDU)
        }
        return rAPDU
    }
}