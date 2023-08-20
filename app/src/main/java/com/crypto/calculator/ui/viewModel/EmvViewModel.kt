package com.crypto.calculator.ui.viewModel

import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.ui.base.BaseViewModel

class EmvViewModel : BaseViewModel() {
    val cardPreference: MutableLiveData<PaymentMethod> = MutableLiveData(PaymentMethod.VISA)
}