package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentEmvBinding
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.ui.viewModel.EmvViewModel

class EmvFragment : MVVMFragment<EmvViewModel, FragmentEmvBinding>() {
    private lateinit var coreViewModel: CoreViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coreViewModel = getCoreViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EmvFragment", "onViewCreated")

        coreViewModel.currentTool.observe(viewLifecycleOwner) {
            Log.d("InputFragment", "currentTool: $it")
            setLayout(it)
        }
    }

    private fun setLayout(tool: Tool) {
        resetLayout()
        when (tool) {
            Tool.CARD_SIMULATOR -> cardSimulator()
            Tool.EMV_KERNEL -> emvKernel()
            else -> {}
        }
    }

    private fun resetLayout() {
        binding.cardContainer.visibility = View.GONE
        binding.animationAwaitCard.visibility = View.GONE
    }

    private fun cardSimulator() {
        binding.cardContainer.visibility = View.VISIBLE
    }

    private fun emvKernel() {
        binding.animationAwaitCard.visibility = View.VISIBLE
    }

    private fun getCoreViewModel(): CoreViewModel {
        Log.d("EmvFragment", "getCoreViewModel")
        return activity?.supportFragmentManager?.fragments?.let { fragmentList ->
            var viewModel: CoreViewModel? = null
            fragmentList.find { it is CoreFragment }?.also {
                viewModel = ViewModelProvider(it, defaultViewModelProviderFactory)[CoreViewModel::class.java]
            }
            viewModel
        } ?: CoreViewModel()
    }

    override fun getViewModelInstance(): EmvViewModel = EmvViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_emv

    override fun screenName(): String = "EmvFragment"
}