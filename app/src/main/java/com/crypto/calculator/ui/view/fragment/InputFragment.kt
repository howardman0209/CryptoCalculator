package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentInputBinding
import com.crypto.calculator.extension.hexBitwise
import com.crypto.calculator.extension.toDataClass
import com.crypto.calculator.model.BitwiseOperation
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewAdapter.DropDownMenuAdapter
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.ui.viewModel.InputViewModel
import com.crypto.calculator.util.ConverterUtil
import com.crypto.calculator.util.HashUtil
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.bindInputFilters
import com.google.gson.JsonObject

class InputFragment : MVVMFragment<InputViewModel, FragmentInputBinding>() {
    private lateinit var coreViewModel: CoreViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coreViewModel = getCoreViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("InputFragment", "onViewCreated")

        viewModel.inputData1InputType.observe(viewLifecycleOwner) {
            binding.etData1.apply {
                inputType = it or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isSingleLine = false
            }
        }

        viewModel.inputData1Filter.observe(viewLifecycleOwner) {
            binding.etData1.bindInputFilters(it)
            viewModel.inputData1.set("")
        }

        viewModel.inputData2InputType.observe(viewLifecycleOwner) {
            binding.etData2.apply {
                inputType = it or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isSingleLine = false
            }
        }

        viewModel.inputData2Filter.observe(viewLifecycleOwner) {
            binding.etData2.bindInputFilters(it)
            viewModel.inputData2.set("")
        }

        viewModel.inputData3InputType.observe(viewLifecycleOwner) {
            binding.etData3.apply {
                inputType = it or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isSingleLine = false
            }
        }

        viewModel.inputData3Filter.observe(viewLifecycleOwner) {
            binding.etData3.bindInputFilters(it)
            viewModel.inputData3.set("")
        }

        coreViewModel.currentTool.observe(viewLifecycleOwner) {
            Log.d("InputFragment", "currentTool: $it")
            setLayout(it)
        }
    }

    private fun setLayout(tool: Tool) {
        resetInput()
        when (tool) {
            Tool.DES -> desCalculator()
            Tool.RSA -> rsaCalculator()
            Tool.AES -> {}
            Tool.MAC -> macCalculator()
            Tool.HASH -> hashCalculator()
            Tool.BITWISE -> bitwiseCalculator()
            Tool.CONVERTER -> converter()
            Tool.TLV_PARSER -> tlvParser()
            else -> {}
        }
    }

    private fun rsaCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Exponent")

        binding.tilData3.visibility = View.VISIBLE
        viewModel.inputData3Label.set("Modulus")

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_compute)
        binding.operationBtn1.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val exponent = viewModel.inputData2.get() ?: ""
            val modulus = viewModel.inputData3.get() ?: ""
            val result = safeExecute {
                viewModel.rsaCompute(data, exponent, modulus)
            }
            coreViewModel.printLog("RSA_CALCULATOR \nData: $data \nExponent: $exponent \nModulus: $modulus \nResult: $result\n")
        }
    }

    private fun desCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Key")
        viewModel.setInputData2Filter(maxLength = 48)

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
        val paddingList = listOf(
            PaddingMethod.ISO9797_1_M1,
            PaddingMethod.ISO9797_1_M2,
        )
        binding.autoTvCondition2.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                paddingList,
            )
        )
        binding.autoTvCondition2.setText(paddingList.first().name)

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_encrypt)

        binding.operationBtn2.visibility = View.VISIBLE
        binding.operationBtn2.text = getString(R.string.label_operation_decrypt)

        binding.operationBtn1.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val key = viewModel.inputData2.get() ?: ""
            val adjustedKey = viewModel.fixDESKeyParity(key)
            val mode = binding.autoTvCondition1.text.toString()
            val padding = binding.autoTvCondition2.text.toString().toDataClass<PaddingMethod>()
            val result = safeExecute {
                viewModel.desEncrypt(
                    data = data,
                    key = key,
                    mode = mode,
                    padding = padding
                )
            }
            Log.d("desCalculator, encrypt", "result: $result")
            coreViewModel.printLog(
                "DES_CALCULATOR \nData: $data \nKey: $adjustedKey " +
                        (if (adjustedKey != key) "(Parity Fixed)" else "") +
                        "\nOperation: Encrypt \nMode: $mode \nPadding: $padding \nEncrypted data: $result\n"
            )
        }

        binding.operationBtn2.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val key = viewModel.inputData2.get() ?: ""
            val adjustedKey = viewModel.fixDESKeyParity(key)
            val mode = binding.autoTvCondition1.text.toString()
            val padding = binding.autoTvCondition2.text.toString().toDataClass<PaddingMethod>()
            val result = safeExecute {
                viewModel.desDecrypt(
                    data = data,
                    key = key,
                    mode = mode,
                    padding = padding
                )
            }
            Log.d("desCalculator, decrypt", "result: $result")
            coreViewModel.printLog(
                "DES_CALCULATOR \nData: $data \nKey: $adjustedKey " +
                        (if (adjustedKey != key) "(Parity Fixed)" else "") +
                        "\nOperation: Decrypt \nMode: $mode \nPadding: $padding \nDecrypted data: $result\n"
            )
        }
    }

    private fun tlvParser() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilCondition1.visibility = View.VISIBLE
        binding.tilCondition1.hint = "Data format"
        val modeList = listOf("TLV", "JSON")
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                modeList,
            )
        )
        binding.autoTvCondition1.setText(modeList.first())
        var selected = 0
        binding.autoTvCondition1.setOnItemClickListener { _, _, i, _ ->
            selected = i
            if (i == modeList.indexOf("TLV")) {
                viewModel.setInputData1Filter(inputFormat = DataFormat.HEXADECIMAL)
                binding.operationBtn1.text = getString(R.string.label_operation_decode)
            } else {
                viewModel.setInputData1Filter(inputFormat = DataFormat.ASCII)
                binding.operationBtn1.text = getString(R.string.label_operation_encode)
            }
        }

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_decode)

        binding.operationBtn1.setOnClickListener {
            viewModel.inputData1.get()?.also { data ->
                when (selected) {
                    0 -> {
                        val result = safeExecute { coreViewModel.gsonBeautifier.toJson(TlvUtil.decodeTLV(data)) }
                        Log.d("tlvParser", "result: $result")
                        coreViewModel.printLog("TLV_PARSER \nTLV: \n$data \nresult: \n$result\n")
                    }

                    else -> {
                        val displayJson = safeExecute {
                            val jsonObj = data.toDataClass<JsonObject>()
                            coreViewModel.gsonBeautifier.toJson(jsonObj)
                        }
                        val result = safeExecute { TlvUtil.encodeTLV(data) }
                        Log.d("tlvParser", "result: $result")
                        coreViewModel.printLog("TLV_PARSER \nJSON: \n$displayJson \nresult: \n$result\n")
                    }
                }
            }
        }
    }

    private fun converter() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        val formatList = listOf(
            DataFormat.HEXADECIMAL,
            DataFormat.BINARY,
            DataFormat.DECIMAL,
            DataFormat.ASCII,
        )
        binding.tilCondition1.visibility = View.VISIBLE
        binding.tilCondition1.hint = "From"
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                formatList,
            )
        )
        binding.autoTvCondition1.setText(formatList.first().name)
        binding.autoTvCondition1.setOnItemClickListener { _, _, i, _ ->
            viewModel.setInputData1Filter(inputFormat = formatList[i])
        }

        binding.tilCondition2.visibility = View.VISIBLE
        binding.tilCondition2.hint = "To"
        binding.autoTvCondition2.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                formatList,
            )
        )
        binding.autoTvCondition2.setText(formatList.last().name)

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_convert)
        binding.operationBtn1.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val fromFormat = binding.autoTvCondition1.text.toString().toDataClass<DataFormat>()
            val toFormat = binding.autoTvCondition2.text.toString().toDataClass<DataFormat>()
            Log.d("converter", "data: $data, from: $fromFormat, to: $toFormat")
            val result = safeExecute { ConverterUtil.convertString(data, fromFormat, toFormat) }
            Log.d("converter", "result: $result")
            coreViewModel.printLog("CONVERTER \nData: $data \nFrom: $fromFormat \nto: $toFormat \nresult: $result\n")
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
            val result = safeExecute { HashUtil.getHexHash(data, algorithm) }
            Log.d("hashCalculator", "algorithm: $algorithm, result: $result")
            coreViewModel.printLog("HASH_CALCULATOR \nData: $data \nAlgorithm: $algorithm \nresult: $result\n")
        }
    }

    private fun macCalculator() {
        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Data")

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Key")
        viewModel.setInputData2Filter(maxLength = 32)

        binding.tilCondition1.visibility = View.VISIBLE
        binding.tilCondition1.hint = "Padding"
        val paddingList = listOf(
            PaddingMethod.ISO9797_1_M1,
            PaddingMethod.ISO9797_1_M2,
        )
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                paddingList,
            )
        )
        binding.autoTvCondition1.setText(paddingList.first().name)

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_encode)
        binding.operationBtn1.setOnClickListener {
            val data = viewModel.inputData1.get() ?: ""
            val key = viewModel.inputData2.get() ?: ""
            val adjustedKey = viewModel.fixDESKeyParity(key)
            val padding = binding.autoTvCondition1.text.toString().toDataClass<PaddingMethod>()
            val result = safeExecute {
                viewModel.macCompute(data, adjustedKey, padding)
            }
            Log.d("macCalculator", "result: $result")
            coreViewModel.printLog(
                "MAC_CALCULATOR \nData: $data \nKey: $adjustedKey " +
                        (if (adjustedKey != key) "(Parity Fixed)" else "") +
                        "\nresult: $result\n"
            )
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
            BitwiseOperation.XOR,
            BitwiseOperation.AND,
            BitwiseOperation.OR,
            BitwiseOperation.NOT,
        )
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                opList,
            )
        )
        var selected = 0
        binding.autoTvCondition1.setText(opList.first().name)
        binding.autoTvCondition1.setOnItemClickListener { _, _, i, _ ->
            selected = i
            if (i == 3) binding.tilData2.visibility = View.GONE else binding.tilData2.visibility = View.VISIBLE
        }

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_encode)
        binding.operationBtn1.setOnClickListener {
            val data1 = viewModel.inputData1.get() ?: ""
            val data2 = viewModel.inputData2.get() ?: ""
            val operation = opList[selected]
            val result = safeExecute {
                data1.hexBitwise(data2, operation)
            }
            Log.d("bitwiseCalculator", "result: $result")
            coreViewModel.printLog(
                "BITWISE_CALCULATOR \n" +
                        "Operation: $operation \nData 1: $data1 " +
                        (if (selected != 3) "\nData 2: $data2 " else "") +
                        "\nresult: $result\n"
            )
        }
    }

    private fun resetInput() {
        Log.d("InputFragment", "resetInput")
        binding.tilData1.visibility = View.GONE
        binding.tilData2.visibility = View.GONE
        binding.tilData3.visibility = View.GONE

        binding.operationBtn1.visibility = View.GONE
        binding.operationBtn1.setOnClickListener(null)
        binding.operationBtn2.visibility = View.GONE
        binding.operationBtn2.setOnClickListener(null)

        binding.tilCondition1.visibility = View.GONE
        binding.tilCondition2.visibility = View.GONE

        binding.autoTvCondition1.onItemClickListener = null
        binding.autoTvCondition2.onItemClickListener = null

        viewModel.setInputData1Filter()
        viewModel.inputData1Label.set("")

        viewModel.setInputData2Filter()
        viewModel.inputData2Label.set("")

        viewModel.setInputData3Filter()
        viewModel.inputData3Label.set("")
    }

    private fun safeExecute(task: () -> String): String {
        return try {
            task.invoke()
        } catch (ex: Exception) {
            "Error: ${ex.message ?: ex}"
        }
    }

    private fun getCoreViewModel(): CoreViewModel {
        Log.d("InputFragment", "getCoreViewModel")
        return activity?.supportFragmentManager?.fragments?.let { fragmentList ->
            var viewModel: CoreViewModel? = null
            fragmentList.find { it is CoreFragment }?.also {
                viewModel = ViewModelProvider(it, defaultViewModelProviderFactory)[CoreViewModel::class.java]
            }
            viewModel
        } ?: CoreViewModel()
    }

    override fun getViewModelInstance(): InputViewModel = InputViewModel()

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_input

    override fun screenName(): String = "InputFragment"
}