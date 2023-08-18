package com.crypto.calculator.model

import com.crypto.calculator.R

enum class Category(val id: Int, val resourceId: Int) {
    UNKNOWN(0, R.string.label_unknown),
    GENERIC(1, R.string.label_category_generic),
    EMV(2, R.string.label_category_emv),
    ;

    companion object {
        fun getById(id: Int): Category {
            return when (id) {
                1 -> GENERIC
                2 -> EMV
                else -> UNKNOWN
            }
        }
    }
}
