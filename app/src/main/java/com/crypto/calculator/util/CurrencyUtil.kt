package com.crypto.calculator.util

import java.util.Currency

object CurrencyUtil {
    fun getCurrencyByNumericCode(numericString: String): Currency {
        val numericCode = numericString.toInt()
        val currencies: Set<Currency> = Currency.getAvailableCurrencies()
        for (currency in currencies) {
            if (currency.numericCode == numericCode) {
                return currency
            }
        }
        throw IllegalArgumentException("Currency with numeric code $numericCode not found")
    }
}