package com.crypto.calculator.util

import android.text.InputFilter
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputEditText

@BindingAdapter("inputFilters")
fun TextView.bindInputFilters(inputFilters: List<InputFilter>) {
    this.filters = arrayOf(
        *this.filters,
        *inputFilters.toTypedArray()
    )
}

@BindingAdapter("inputMaxLength")
fun TextInputEditText.bindInputMaxLength(inputMaxLength: Int?) {
    if (inputMaxLength != null) {
        this.filters = arrayOf(
            *this.filters,
            InputFilter.LengthFilter(inputMaxLength)
        )
    }
}