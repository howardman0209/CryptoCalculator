package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentInputBinding
import com.crypto.calculator.extension.hexBitwise
import com.crypto.calculator.model.BitwiseOperation
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewAdapter.DropDownMenuAdapter
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.util.HashUtil
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.encryption_padding_iso9797_1_M1
import com.crypto.calculator.util.encryption_padding_iso9797_1_M2

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
            Tool.DES -> desCalculator()

            Tool.AES -> {}

            Tool.MAC -> {}

            Tool.HASH -> hashCalculator()

            Tool.BITWISE -> bitwiseCalculator()

            Tool.CONVERTER -> {}

            Tool.TLV_PARSER -> tlvParser()
        }
    }

    private fun desCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Key")
        viewModel.inputData2Max.set(32)

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
        val paddingList = listOf(encryption_padding_iso9797_1_M1, encryption_padding_iso9797_1_M2)
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

        binding.operationBtn1.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val key = viewModel.inputData2.get() ?: ""
            val mode = binding.autoTvCondition1.text.toString()
            val padding = binding.autoTvCondition2.text.toString()
            val result = viewModel.desEncrypt(
                data = data,
                key = key,
                mode = mode,
                padding = padding
            )
            Log.d("desCalculator, encrypt", "result: $result")
            viewModel.printLog("DES_CALCULATOR \nData: $data \nKey: $key \nOperation: Encrypt \nMode: $mode \nPadding: $padding \nEncrypted data: $result\n")
        }

        binding.operationBtn2.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val key = viewModel.inputData2.get() ?: ""
            val mode = binding.autoTvCondition1.text.toString()
            val padding = binding.autoTvCondition2.text.toString()
            val result = viewModel.desDecrypt(
                data = data,
                key = key,
                mode = mode,
                padding = padding
            )
            Log.d("desCalculator, decrypt", "result: $result")
            viewModel.printLog("DES_CALCULATOR \nData: $data \nKey: $key \nOperation: Decrypt \nMode: $mode \nDecrypted data: $result\n")
        }
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
                viewModel.printLog("TLV_PARSER \nTLV: $tlv \n$result\n")
            }
        }
    }

    private fun hashCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilCondition1.visibility = View.VISIBLE
        binding.tilCondition1.hint = "Algorithm"
        val algoList = listOf("SHA-1", "SHA-224", "SHA-256", "MD5")
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                algoList,
            )
        )
        binding.autoTvCondition1.setText(algoList.first())

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_encode)
        binding.operationBtn1.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val algorithm = binding.autoTvCondition1.text.toString()
            val result = try {
                HashUtil.getHexHash(data, algorithm)
            } catch (e: Exception) {
                e.message
            }
            Log.d("hashCalculator", "algorithm: $algorithm, result: $result")
            viewModel.printLog("HASH_CALCULATOR \nData: $data \nAlgorithm: $algorithm \nresult: $result\n")
        }
    }

    private fun bitwiseCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Block A")

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Block B")

        binding.tilCondition1.visibility = View.VISIBLE
        binding.tilCondition1.hint = "Operation"
        val opList = listOf(
            BitwiseOperation.XOR.name,
            BitwiseOperation.AND.name,
            BitwiseOperation.OR.name,
            BitwiseOperation.NOT.name,
        )
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                opList,
            )
        )
        var selected = 0
        binding.autoTvCondition1.setText(opList.first())
        binding.autoTvCondition1.setOnItemClickListener { _, _, i, _ ->
            selected = i
            if (i == 3) binding.tilData2.visibility = View.GONE else binding.tilData2.visibility = View.VISIBLE
        }

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_encode)
        binding.operationBtn1.setOnClickListener {
            val data1 = viewModel.inputData1.get() ?: ""
            val data2 = viewModel.inputData2.get() ?: ""
            val operation = when (selected) {
                0 -> BitwiseOperation.XOR
                1 -> BitwiseOperation.AND
                2 -> BitwiseOperation.OR
                else -> BitwiseOperation.NOT
            }
            val result = try {
                Log.d("bitwiseCalculator", "operation: $operation")
                data1.hexBitwise(data2, operation)
            } catch (e: Exception) {
                e.message
            }
            Log.d("bitwiseCalculator", "result: $result")
            viewModel.printLog(
                "BITWISE_CALCULATOR \n" +
                        "Operation: $operation \nData 1: $data1 " +
                        (if (selected != 3) "\nData 2: $data2 " else "") +
                        "\nresult: $result\n"
            )
        }
    }

    private fun resetInput() {
        binding.tilData1.visibility = View.GONE
        binding.tilData2.visibility = View.GONE
        binding.tilData2.tag = null

        binding.operationBtn1.visibility = View.GONE
        binding.operationBtn1.setOnClickListener(null)
        binding.operationBtn2.visibility = View.GONE
        binding.operationBtn2.setOnClickListener(null)

        binding.tilCondition1.visibility = View.GONE
        binding.tilCondition2.visibility = View.GONE

        binding.autoTvCondition1.onItemClickListener = null
        binding.autoTvCondition2.onItemClickListener = null

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