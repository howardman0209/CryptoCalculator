package com.crypto.calculator.model

data class IssuerMasterKeyList(
    val data: HashMap<PaymentMethod, String>? = null
)