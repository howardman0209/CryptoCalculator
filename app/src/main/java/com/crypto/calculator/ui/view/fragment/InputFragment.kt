package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentInputBinding
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewAdapter.DropDownMenuAdapter
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.util.TlvUtil

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
                desCalculator()
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

    private fun desCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Key")

        binding.tilCondition1.visibility = View.VISIBLE
        binding.tilCondition1.hint = "Mode"
        val modeList = listOf("ECB")
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                modeList,
            )
        )
        binding.autoTvCondition1.setText(modeList.first())

        binding.tilCondition2.visibility = View.VISIBLE
        binding.tilCondition2.hint = "Padding"
        val paddingList = listOf("Method 1", "Method 2")
        binding.autoTvCondition2.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                paddingList,
            )
        )
        binding.autoTvCondition2.setText(paddingList.first())

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_encrypt)

        binding.operationBtn2.visibility = View.VISIBLE
        binding.operationBtn2.text = getString(R.string.label_operation_decrypt)


    }

    private fun tlvParser() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_decode)

        binding.operationBtn1.setOnClickListener {
            viewModel.inputData1.get()?.also { tlv ->
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
        binding.tilData1.visibility = View.GONE
        binding.tilData2.visibility = View.GONE
        binding.tilData2.tag = null

        binding.operationBtn1.visibility = View.GONE
        binding.operationBtn2.visibility = View.GONE

        binding.tilCondition1.visibility = View.GONE

        viewModel.inputData1.set("")
        viewModel.inputData1Max.set(null)
        viewModel.inputData1Label.set("")

        viewModel.inputData2.set("")
        viewModel.inputData2Max.set(null)
        viewModel.inputData2Label.set("")
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