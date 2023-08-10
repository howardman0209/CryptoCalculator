package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentOutputBinding
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OutputFragment : MVVMFragment<MainViewModel, FragmentOutputBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLogPanel()
        startPrintTest()
    }

    private fun startPrintTest() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                delay(1000)
                printLog("Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World ")
            }
        }
    }

    private fun initLogPanel() {
        binding.logPanel.movementMethod = ScrollingMovementMethod()
    }

    fun printLog(logStr: String) {
        val vlog = String.format("%s: %s", viewModel.getCurrentDateTime(true), logStr)
        viewModel.logMessage.get().also {
            if (it.isNullOrEmpty()) {
                viewModel.logMessage.set(vlog)
            } else {
                viewModel.logMessage.set("$it\n$vlog")
            }
        }
        Log.d("LogPanel", logStr)
    }

    override fun getViewModelInstance(): MainViewModel {
        return activity?.run {
            ViewModelProvider(this, defaultViewModelProviderFactory)[MainViewModel::class.java]
        } ?: MainViewModel()
    }

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_output

    override fun screenName(): String = "OutputFragment"
}