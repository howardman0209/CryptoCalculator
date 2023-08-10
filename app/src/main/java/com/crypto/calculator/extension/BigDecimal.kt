package com.crypto.calculator.extension

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/**
 * Format BigDecimal to localized string with ISO-4217 currency code (eg. HKD, USD, EUR) or symbol (HK$, $, €)
 * For HK$ symbol this fun specially handle to override it to $
 * The currency code uses the currency argument if passed, if null > uses the config currency, if null > HKD
 * @param withCurrency return string with or without currency
 * @param spaceBetween for some regions (primarily US, UK and singapore), there is no space between currency and amount
 * @return Localized string with currency code (eg. HKD 5,000.00)
 */
fun BigDecimal.toCurrencyString(
    currency: String? = null,
    withCurrency: Boolean = true,
    useSymbol: Boolean = true,
    spaceBetween: Boolean = false,
    minFractionDigitsByScale: Boolean = false
): String {
    val mCurrency = Currency.getInstance(currency ?: "HKD")
    val shopCurrency = Currency.getInstance("HKD")
    val simplifiedCurrencySymbol = shopCurrency.symbol.simplifiedCurrencySymbol()
    val locale = Locale("en", "HK")
    val numFormat = NumberFormat.getCurrencyInstance(locale)
        .run {
            this.currency = mCurrency
            if (minFractionDigitsByScale) {
                this.minimumFractionDigits = scale()
            }
            this as DecimalFormat
        }
        .also {
            val sym = it.decimalFormatSymbols
            sym.currencySymbol = ""
            it.decimalFormatSymbols = sym
            it.maximumFractionDigits = mCurrency.defaultFractionDigits
        }
    val numericString = numFormat.format(this.abs())
    val symbolString = if (useSymbol) {
        //special handle for hkd
        if (mCurrency.symbol == shopCurrency.symbol) simplifiedCurrencySymbol else mCurrency.symbol
    } else {
        mCurrency.toString()
    }

    val sign = if (this.signum() >= 0) "" else "-"

    return "$sign${if (withCurrency) symbolString else ""}${if (spaceBetween) " " else ""}${numericString.format(this)}"
}

fun BigDecimal.toGlobalCurrencyString(
    currency: String? = null,
    withCurrency: Boolean = true,
    useSymbol: Boolean = true,
    spaceBetween: Boolean = false,
    minFractionDigitsByScale: Boolean = false
): String {
    val mCurrency = Currency.getInstance(currency ?: "HKD")
    val locale = Locale("en", "HK")
    val numFormat = NumberFormat.getCurrencyInstance(locale)
        .run {
            this.currency = mCurrency
            if (minFractionDigitsByScale) {
                this.minimumFractionDigits = scale()
            }
            this as DecimalFormat
        }
        .also {
            val sym = it.decimalFormatSymbols
            sym.currencySymbol = ""
            it.decimalFormatSymbols = sym
            it.maximumFractionDigits = 2
        }
    val numericString = numFormat.format(this.abs())
    val symbolString = if (useSymbol) {
        mCurrency.symbol
    } else {
        mCurrency.toString()
    }

    val sign = if (this.signum() >= 0) "" else "-"

    return "$sign${if (withCurrency) symbolString else ""}${if (spaceBetween) " " else ""}${numericString.format(this)}"
}

fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
