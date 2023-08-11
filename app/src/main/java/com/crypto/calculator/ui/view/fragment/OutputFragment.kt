package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.core.view.marginBottom
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
//        initLogPanel()
//        startPrintTest()
    }

    private fun startPrintTest() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                delay(1000)
                printLog("Hello World")
//                viewModel.printLog("Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World ")
                scrollToBottom()
            }
        }
    }

    private fun initLogPanel() {
        binding.logPanel.movementMethod = ScrollingMovementMethod()
    }

    fun printLog(logStr: String) {
        val vlog = String.format("%s: %s\n", viewModel.getCurrentDateTime(true), logStr)
        binding.logPanel.append(vlog)
        Log.d("LogPanel", logStr)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding.verticalScrollView.apply {
            getChildAt(childCount - 1)
            val bottom = binding.logPanel.bottom + marginBottom
            val currentY = measuredHeight + scrollY
            Log.d("ScrollView", "currentY: $currentY")
            val alreadyAtBottom = bottom <= currentY
            Log.d("ScrollView", "already at bottom: $alreadyAtBottom")
            if (!alreadyAtBottom) {
                val delta = bottom - currentY
                smoothScrollBy(0, delta)
                Log.d("ScrollView", "scroll to bottom")
            } else {
                // already at bottom, do nothing
                Log.d("ScrollView", "do nothing")
            }
        }
    }

//    fun ScrollView.scrollToBottom() {
//        val lastChild = getChildAt(childCount - 1)
//        val bottom = lastChild.bottom + paddingBottom
//        val delta = bottom - (scrollY + height)
//        smoothScrollBy(0, delta)
//    }

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