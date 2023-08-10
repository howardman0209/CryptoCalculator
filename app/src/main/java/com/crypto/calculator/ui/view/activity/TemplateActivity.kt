package com.crypto.calculator.ui.view.activity

import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityTemplateBinding
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.viewModel.TemplateViewModel

class TemplateActivity: MVVMActivity<TemplateViewModel,ActivityTemplateBinding>() {


    override fun getViewModelInstance(): TemplateViewModel = TemplateViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_template
}