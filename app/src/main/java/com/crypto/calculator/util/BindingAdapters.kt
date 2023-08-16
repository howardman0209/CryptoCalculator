package com.crypto.calculator.util

import android.text.InputFilter
import android.util.Log
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputEditText


@BindingAdapter("inputFilters")
fun TextInputEditText.bindInputFilters(inputFilters: List<InputFilter>) {
    if (inputFilters.isNotEmpty()) {
        this.filters = arrayOf(
            *this.filters,
            *inputFilters.toTypedArray()
        )
    } else {
        this.filters = emptyArray()
    }
}