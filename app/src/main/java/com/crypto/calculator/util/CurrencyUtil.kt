package com.crypto.calculator.util

import java.util.Currency

object CurrencyUtil {
    fun getCurrentShopCurrencyDecimal(): Int {
        return Currency.getInstance().defaultFractionDigits
    }
}