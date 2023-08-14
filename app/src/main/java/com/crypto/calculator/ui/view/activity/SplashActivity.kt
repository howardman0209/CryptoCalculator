package com.crypto.calculator.ui.view.activity

import android.content.Intent
import android.os.Bundle
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivitySplashBinding
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.viewModel.SplashViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity:MVVMActivity<SplashViewModel, ActivitySplashBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    override fun getViewModelInstance(): SplashViewModel  = SplashViewModel()
    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_splash
}