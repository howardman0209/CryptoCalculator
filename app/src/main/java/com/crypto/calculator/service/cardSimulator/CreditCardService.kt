package com.crypto.calculator.service.cardSimulator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.handler.Event
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.service.cardSimulator.delegate.AmexDelegate
import com.crypto.calculator.service.cardSimulator.delegate.DiscoverDelegate
import com.crypto.calculator.service.cardSimulator.delegate.JcbDelegate
import com.crypto.calculator.service.cardSimulator.delegate.MastercardDelegate
import com.crypto.calculator.service.cardSimulator.delegate.UnionPayDelegate
import com.crypto.calculator.service.cardSimulator.delegate.VisaDelegate
import com.crypto.calculator.util.PreferencesUtil
import com.crypto.calculator.util.assetsPathCardAmex
import com.crypto.calculator.util.assetsPathCardDiscover
import com.crypto.calculator.util.assetsPathCardJcb
import com.crypto.calculator.util.assetsPathCardMaster
import com.crypto.calculator.util.assetsPathCardUnionPay
import com.crypto.calculator.util.assetsPathCardVisa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreditCardService : BasicEMVService() {
    companion object {
        private val _apdu = MutableLiveData<Event<String>>()
        val apdu: LiveData<Event<String>>
            get() = _apdu

        val status: MutableLiveData<CardSimulatorStatus> = MutableLiveData()

        enum class CardSimulatorStatus {
            START,
            PROCESSING,
            END
        }

        fun requestDefaultPaymentServiceIntent(context: Context): Intent {
            val intent = Intent().apply {
                action = CardEmulation.ACTION_CHANGE_DEFAULT
                putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, ComponentName(context, CreditCardService::class.java))
                putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT)
            }
            return intent
        }

        fun isDefaultPaymentService(context: Context): Boolean {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            val paymentServiceName = ComponentName(context, CreditCardService::class.java)
//            Log.d("CreditCardSimulator", "paymentServiceName: $paymentServiceName")
            return cardEmulation.isDefaultServiceForCategory(paymentServiceName, CardEmulation.CATEGORY_PAYMENT)
        }

        fun enablePaymentService(context: Context, enable: Boolean = true) {
            val flag = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, CreditCardService::class.java),
                flag,
                PackageManager.DONT_KILL_APP
            )
        }

        fun getDefaultCardAssetsPath(cardType: PaymentMethod): String {
            val path = when (cardType) {
                PaymentMethod.VISA -> "${assetsPathCardVisa}_cvn10.json"
                PaymentMethod.MASTER -> "${assetsPathCardMaster}.json"
                PaymentMethod.UNIONPAY -> assetsPathCardUnionPay
                PaymentMethod.JCB -> "${assetsPathCardJcb}_emv.json"
                PaymentMethod.DISCOVER -> assetsPathCardDiscover
                PaymentMethod.AMEX -> assetsPathCardAmex
                else -> ""
            }
            return path
        }
    }

    override fun onCreate() {
        status.value = CardSimulatorStatus.START
        super.onCreate()
        Log.d("CreditCardSimulator", "CardPreference: ${PreferencesUtil.getCardPreference(applicationContext)}")
        emvFlowDelegate = cardFactory(PreferencesUtil.getCardPreference(applicationContext))
    }

    private fun cardFactory(cardScheme: PaymentMethod): EMVFlowDelegate {
        val cardProfile = PreferencesUtil.getCardProfile(applicationContext, cardScheme)
        return when (cardScheme) {
            PaymentMethod.VISA -> VisaDelegate.getInstance(applicationContext, cardProfile.data)
            PaymentMethod.MASTER -> MastercardDelegate.getInstance(applicationContext, cardProfile.data)
            PaymentMethod.UNIONPAY -> UnionPayDelegate.getInstance(applicationContext, cardProfile.data)
            PaymentMethod.JCB -> JcbDelegate.getInstance(applicationContext, cardProfile.data)
            PaymentMethod.DISCOVER -> DiscoverDelegate.getInstance(applicationContext, cardProfile.data)
            PaymentMethod.AMEX -> AmexDelegate.getInstance(applicationContext, cardProfile.data)
            else -> VisaDelegate.getInstance(applicationContext, cardProfile.data)
        }
    }

    override fun processCommandApdu(p0: ByteArray?, p1: Bundle?): ByteArray {
        status.value = CardSimulatorStatus.PROCESSING
        return super.processCommandApdu(p0, p1)
    }

    override fun responseConstructor(cAPDU: String?): String {
        val rAPDU = super.responseConstructor(cAPDU)
        CoroutineScope(Dispatchers.Main).launch {
            cAPDU?.also { _apdu.value = Event(it) }
            _apdu.value = Event(rAPDU)
        }
        return rAPDU
    }

    override fun onDestroy() {
        super.onDestroy()
        status.value = CardSimulatorStatus.END
    }
}