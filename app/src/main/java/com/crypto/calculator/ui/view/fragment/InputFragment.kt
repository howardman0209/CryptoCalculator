package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentInputBinding
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.util.TlvUtil
import com.google.gson.Gson

class InputFragment : MVVMFragment<CoreViewModel, FragmentInputBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("InputFragment", "onViewCreated")

        viewModel.currentTool.observe(viewLifecycleOwner) {
            Log.d("InputFragment", "currentTool: $it")
            setLayout(it)
        }
    }

    private fun setLayout(tool: Tool) {
        resetInput()
        when (tool) {
            Tool.DES -> {

            }

            Tool.AES -> {

            }

            Tool.MAC -> {

            }

            Tool.HASH -> {

            }

            Tool.BITWISE -> {

            }

            Tool.CONVERTER -> {

            }

            Tool.TLV_PARSER -> {
                tlvParser()
            }
        }
    }

    private fun tlvParser() {
        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_decode)

        binding.operationBtn1.setOnClickListener {
            viewModel.inputData.get()?.also { tlv ->
                val result = try {
                    viewModel.gsonBeautifier.toJson(TlvUtil.decodeTLV(tlv))
                } catch (ex: Exception) {
                    ex.message
                }
                Log.d("tlvParser", "result: $result")
                viewModel.printLog("TLV_PARSER \nTLV: $tlv \n$result")
            }
        }
    }

    private fun resetInput() {
        binding.operationBtn1.visibility = View.GONE
        binding.operationBtn2.visibility = View.GONE
        viewModel.inputData.set("")
        viewModel.inputDataMax.set(null)
        viewModel.inputKey.set("")
        viewModel.inputKeyMax.set(null)
    }

    override fun getViewModelInstance(): CoreViewModel {
        return activity?.supportFragmentManager?.fragments?.let { fragmentList ->
            var viewModel: CoreViewModel? = null
            fragmentList.find { it is CoreFragment }?.also {
                viewModel = ViewModelProvider(it, defaultViewModelProviderFactory)[CoreViewModel::class.java]
            }
            viewModel
        } ?: CoreViewModel()
    }

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_input

    override fun screenName(): String = "InputFragment"
}