package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.marginBottom
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentOutputBinding
import com.crypto.calculator.extension.requireManageFilePermission
import com.crypto.calculator.ui.base.BaseActivity
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.ui.viewModel.OutputViewModel
import com.crypto.calculator.util.LogPanelUtil
import com.crypto.calculator.util.ShareFileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OutputFragment : MVVMFragment<OutputViewModel, FragmentOutputBinding>() {
    private lateinit var coreViewModel: CoreViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coreViewModel = getCoreViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        LogPanelUtil.logMessage.observe(viewLifecycleOwner) {
            printLog(it)
        }

        viewModel.warpText.observe(viewLifecycleOwner) {
            if (!it) binding.logPanel.maxWidth = Int.MAX_VALUE else binding.logPanel.maxWidth = binding.horizontalScrollView.measuredWidth
        }

        binding.wrapBtn.setOnClickListener {
            Log.d("wrapBtn", "OnClick")
            viewModel.warpText.apply {
                postValue(this.value != true)
            }
        }

        binding.saveBtn.setOnClickListener {
            Log.d("saveBtn", "OnClick")
            (requireActivity() as BaseActivity).requireManageFilePermission(it) {
                if (binding.logPanel.text.toString().isNotEmpty()) {
                    singleInputDialog(requireContext(), "Please input a file suffix", "Suffix") { suffix ->
                        if (suffix.isNotEmpty()) {
                            ShareFileUtil.saveLogToFile(requireContext(), binding.logPanel.text.toString(), suffix)
                        }
                    }
                }
            }
        }

        binding.clearBtn.setOnClickListener {
            Log.d("clearBtn", "OnClick")
            binding.logPanel.text = ""
        }

        binding.copyBtn.setOnClickListener {
            Log.d("copyBtn", "OnClick")
            copyTextToClipboard(requireContext(), binding.logPanel.text.toString())
        }
    }

    private fun startPrintTest() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                delay(1000)
                printLog("Hello World")
                scrollToBottom()
            }
        }
    }

    private fun printLog(logStr: String) {
        val vlog = String.format("%s %s\n", viewModel.getCurrentDateTime(true), logStr)
        binding.logPanel.append(vlog)
        Log.d("LogPanel", logStr)
        binding.logPanel.post {
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        binding.verticalScrollView.apply {
            binding.logPanel.clearFocus() // avoid select text to change the focus, ensure focus at the last line
            val bottom = binding.logPanel.bottom + marginBottom
            val currentY = measuredHeight + scrollY
            val alreadyAtBottom = bottom <= currentY
            Log.d("ScrollView", "already at bottom: $alreadyAtBottom")
            if (!alreadyAtBottom) {
                smoothScrollTo(0, bottom)
                Log.d("ScrollView", "scroll to bottom")
            } else {
                // already at bottom, do nothing
                Log.d("ScrollView", "do nothing")
            }
        }
    }

    private fun getCoreViewModel(): CoreViewModel {
        Log.d("OutputFragment", "getCoreViewModel")
        return activity?.supportFragmentManager?.fragments?.let { fragmentList ->
            var viewModel: CoreViewModel? = null
            fragmentList.find { it is CoreFragment }?.also {
                viewModel = ViewModelProvider(it, defaultViewModelProviderFactory)[CoreViewModel::class.java]
            }
            viewModel
        } ?: CoreViewModel()
    }

    override fun getViewModelInstance(): OutputViewModel = OutputViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_output

    override fun screenName(): String = "OutputFragment"
}