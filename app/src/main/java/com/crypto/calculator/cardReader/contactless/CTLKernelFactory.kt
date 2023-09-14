package com.crypto.calculator.cardReader.contactless

import com.crypto.calculator.cardReader.EmvKernel
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.util.EMVUtils

object CTLKernelFactory {
    fun create(core: EmvKernel): BasicCTLKernel? {
        return core.getICCTag("9F2A")?.let { kernelID ->
            when (kernelID.toInt()) {
                1 -> EMVCTLKernel0(core)
                else -> EMVCTLKernel0(core)
            }
        } ?: core.getICCTag("4F")?.let { aid ->
            when (EMVUtils.getPaymentMethodByAID(aid)) {
                PaymentMethod.MASTER -> EMVCTLKernel0(core)
                PaymentMethod.VISA -> EMVCTLKernel0(core)
                PaymentMethod.AMEX -> EMVCTLKernel0(core)
                PaymentMethod.JCB -> EMVCTLKernel0(core)
                PaymentMethod.DINERS, PaymentMethod.DISCOVER -> EMVCTLKernel0(core)
                PaymentMethod.UNIONPAY -> EMVCTLKernel0(core)
                else -> EMVCTLKernel0(core)
            }
        }
    }
}