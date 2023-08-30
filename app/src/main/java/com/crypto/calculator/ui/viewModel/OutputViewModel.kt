package com.crypto.calculator.ui.viewModel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.ui.base.BaseViewModel

class OutputViewModel : BaseViewModel() {
    val warpText = MutableLiveData(false)
    var isSearching = ObservableField(false)
}