package com.crypto.calculator.ui.viewModel

import androidx.databinding.ObservableField
import com.crypto.calculator.ui.base.BaseViewModel

class MainViewModel : BaseViewModel() {
    val title: ObservableField<String> = ObservableField()
    val logMessage: ObservableField<String> = ObservableField()
}