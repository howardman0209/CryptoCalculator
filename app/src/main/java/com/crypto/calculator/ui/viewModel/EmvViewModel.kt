package com.crypto.calculator.ui.viewModel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.ui.base.BaseViewModel

class EmvViewModel : BaseViewModel() {
    val promptMessage: ObservableField<String> = ObservableField()
    val cardPreference: MutableLiveData<PaymentMethod> = MutableLiveData(PaymentMethod.VISA)
}