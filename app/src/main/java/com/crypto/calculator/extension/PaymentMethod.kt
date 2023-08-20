package com.crypto.calculator.extension

import com.crypto.calculator.R
import com.crypto.calculator.model.PaymentMethod

/**
 * get PaymentMethod icon res id
 */
fun PaymentMethod.getColorIconResId(): Int {
    return when (this) {
        PaymentMethod.VISA -> R.drawable.acceptance_paymentmethod_visa
        PaymentMethod.MASTER -> R.drawable.acceptance_paymentmethod_mastercard
        PaymentMethod.JCB -> R.drawable.acceptance_paymentmethod_jcb
        PaymentMethod.UNIONPAY -> R.drawable.acceptance_paymentmethod_unionpay
        PaymentMethod.UPI_QR -> R.drawable.acceptance_paymentmethod_upiqrapp
        PaymentMethod.AMEX -> R.drawable.acceptance_paymentmethod_amex
        PaymentMethod.ALIPAY -> R.drawable.acceptance_paymentmethod_alipay
        PaymentMethod.WECHAT -> R.drawable.acceptance_paymentmethod_wechatpay
        PaymentMethod.FPS -> R.drawable.acceptance_paymentmethod_fps
        PaymentMethod.OCTOPUS -> R.drawable.acceptance_paymentmethod_octopus
        PaymentMethod.CASH -> R.drawable.acceptance_paymentmethod_cash
        PaymentMethod.UNKNOWN -> R.drawable.acceptance_paymentmethod_unknown
        PaymentMethod.DINERS -> R.drawable.acceptance_paymentmethod_diners
        PaymentMethod.DISCOVER -> R.drawable.acceptance_paymentmethod_discover
        PaymentMethod.EPS -> R.drawable.acceptance_paymentmethod_eps
        PaymentMethod.ZELLE -> R.drawable.acceptance_paymentmethod_zelle
        PaymentMethod.MAESTRO -> R.drawable.acceptance_paymentmethod_maestro
        PaymentMethod.EASYCARD, PaymentMethod.EASYCARD_QR -> R.drawable.acceptance_paymentmethod_easycard
        PaymentMethod.PAYME -> R.drawable.acceptance_paymentmethod_payme
        PaymentMethod.YUU -> R.drawable.acceptance_paymentmethod_yuu
        else -> R.drawable.acceptance_paymentmethod_unknown
    }
}

/**
 * get PaymentMethod string res id
 */
fun PaymentMethod.getStringResId(): Int {
    return when (this) {
        PaymentMethod.VISA -> R.string.payment_method_visa
        PaymentMethod.MASTER -> R.string.payment_method_master
        PaymentMethod.JCB -> R.string.payment_method_jcb
        PaymentMethod.UNIONPAY -> R.string.payment_method_cup
        PaymentMethod.UPI_QR -> R.string.payment_method_upi_qr
        PaymentMethod.AMEX -> R.string.payment_method_amex
        PaymentMethod.ALIPAY -> R.string.payment_method_alipay
        PaymentMethod.WECHAT -> R.string.payment_method_wechat
        PaymentMethod.FPS -> R.string.payment_method_fps
        PaymentMethod.OCTOPUS -> R.string.payment_method_octopus
        PaymentMethod.CASH -> R.string.payment_method_cash
        PaymentMethod.UNKNOWN -> R.string.label_unknown
        PaymentMethod.DINERS -> R.string.payment_method_diners
        PaymentMethod.DISCOVER -> R.string.payment_method_discover
        PaymentMethod.EPS -> R.string.payment_method_eps
        PaymentMethod.ZELLE -> R.string.payment_method_zelle
        PaymentMethod.MAESTRO -> R.string.payment_method_maestro
        PaymentMethod.EASYCARD, PaymentMethod.EASYCARD_QR -> R.string.payment_method_easycard
        PaymentMethod.PAYME -> R.string.payment_method_payme
        PaymentMethod.YUU -> R.string.payment_method_yuu
        else -> R.string.label_unknown
    }
}

/**
 * get PaymentMethod color res id for chart display
 */
fun PaymentMethod.getColorResId(): Int {
    return when (this) {
        PaymentMethod.VISA -> R.color.brand_visa
        PaymentMethod.MASTER -> R.color.brand_mastercard
        PaymentMethod.JCB -> R.color.brand_jcb
        PaymentMethod.UNIONPAY, PaymentMethod.UPI_QR -> R.color.brand_unionpay
        PaymentMethod.AMEX -> R.color.brand_amex
        PaymentMethod.ALIPAY -> R.color.brand_alipay
        PaymentMethod.WECHAT -> R.color.brand_wechat
        PaymentMethod.FPS -> R.color.brand_fps
        PaymentMethod.PAYME -> R.color.brand_payme
        PaymentMethod.OCTOPUS -> R.color.brand_octopus
        PaymentMethod.CASH -> R.color.brand_cash
        PaymentMethod.YUU -> R.color.brand_yuu
        else -> R.color.so_dark_a64
    }
}