package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.cardReader.BasicCardReader
import com.crypto.calculator.cardReader.EMVKernel
import com.crypto.calculator.databinding.FragmentEmvBinding
import com.crypto.calculator.extension.getColorIconResId
import com.crypto.calculator.extension.requireDefaultPaymentServicePermission
import com.crypto.calculator.extension.toDataClass
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.model.Tool
import com.crypto.calculator.service.cardSimulator.CreditCardSimulator
import com.crypto.calculator.service.model.CardProfile
import com.crypto.calculator.ui.base.BaseActivity
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewAdapter.DropDownMenuAdapter
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.ui.viewModel.EmvViewModel
import com.crypto.calculator.util.AssetsUtil
import com.crypto.calculator.util.PreferencesUtil
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.assetsPathCardVisa
import com.crypto.calculator.util.bindInputFilters

class EmvFragment : MVVMFragment<EmvViewModel, FragmentEmvBinding>() {
    private lateinit var coreViewModel: CoreViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coreViewModel = getCoreViewModel()
        viewModel.cardPreference.value = PreferencesUtil.getCardPreference(requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EmvFragment", "onViewCreated")

        coreViewModel.currentTool.observe(viewLifecycleOwner) {
            Log.d("EmvFragment", "currentTool: $it")
            CreditCardSimulator.enablePaymentService(requireContext().applicationContext, false)
            setLayout(it)
        }

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
    }

    private fun setLayout(tool: Tool) {
        resetLayout()
        when (tool) {
            Tool.CARD_SIMULATOR -> cardSimulator()
            Tool.EMV_KERNEL -> emvKernel()
            Tool.ARQC -> arqcCalculator()
            else -> {}
        }
    }

    private fun cardSimulator() {
        CreditCardSimulator.enablePaymentService(requireContext().applicationContext, true)
        binding.tvPrompt.visibility = View.VISIBLE

        (requireActivity() as BaseActivity).requireDefaultPaymentServicePermission {
            binding.cardContainer.visibility = View.VISIBLE
            viewModel.promptMessage.set(getString(R.string.label_present_card_to_reader))
            viewModel.cardPreference.observe(viewLifecycleOwner) {
                binding.ivPaymentMethod.setImageResource(it.getColorIconResId())
            }

            CreditCardSimulator.apdu.observe(viewLifecycleOwner) { apdu ->
                Log.d("cardSimulator", "apdu: $apdu")
                if (!binding.opt1CheckBox.isChecked) {
                    apdu?.let { coreViewModel.printLog(it) }
                } else {
                    apdu?.let {
                        viewModel.getInspectLog(it)
                    }?.apply {
                        coreViewModel.printLog(this)
                    }
                }
            }

            binding.ivPaymentMethod.setOnClickListener {
                arrayItemDialog(
                    context = requireContext(),
                    items = arrayOf(
                        PaymentMethod.VISA.officialName,
                        PaymentMethod.MASTER.officialName,
                        PaymentMethod.UNIONPAY.officialName,
                        PaymentMethod.JCB.officialName,
                        PaymentMethod.DISCOVER.officialName,
                        PaymentMethod.AMEX.officialName,
                    ),
                    title = getString(R.string.label_select_card),
                ) { selected ->
                    val card = when (selected) {
                        0 -> PaymentMethod.VISA
                        1 -> PaymentMethod.MASTER
                        2 -> PaymentMethod.UNIONPAY
                        3 -> PaymentMethod.JCB
                        4 -> PaymentMethod.DISCOVER
                        5 -> PaymentMethod.AMEX
                        else -> PaymentMethod.UNKNOWN
                    }
                    viewModel.cardPreference.postValue(card)
                    Log.d("ivPaymentMethod", "cardPreference: $card")
                    PreferencesUtil.saveCardPreference(requireContext().applicationContext, card)
                }
            }

            binding.ivCard.setOnClickListener {
                val cardPreference = PreferencesUtil.getCardPreference(requireContext().applicationContext)
                Log.d("ivCardProfile", "cardPreference: $cardPreference")
                when (cardPreference) {
                    PaymentMethod.VISA -> {
                        editConfigJson(
                            view = binding.root,
                            context = requireContext(),
                            config = PreferencesUtil.getCardProfile(requireContext().applicationContext, cardPreference),
                            onConfirmClick = {
                                PreferencesUtil.saveCardProfile(requireContext().applicationContext, it, cardPreference)
                            },
                            neutralBtn = getString(R.string.button_reset),
                            onNeutralBtnClick = {
                                selectVisaCardProfile()
                            }
                        )
                    }

                    else -> {
                        editConfigJson(
                            view = binding.root,
                            context = requireContext(),
                            config = PreferencesUtil.getCardProfile(requireContext().applicationContext, cardPreference),
                            onConfirmClick = {
                                PreferencesUtil.saveCardProfile(requireContext().applicationContext, it, cardPreference)
                            },
                            neutralBtn = getString(R.string.button_reset),
                            onNeutralBtnClick = {
                                PreferencesUtil.saveCardProfile(
                                    requireContext().applicationContext,
                                    AssetsUtil.readFile(
                                        requireContext().applicationContext,
                                        CreditCardSimulator.getDefaultCardAssetsPath(cardPreference)
                                    ),
                                    cardPreference
                                )
                            }
                        )
                    }
                }
            }

            binding.opt1CheckBox.visibility = View.VISIBLE
            binding.opt1CheckBox.text = getString(R.string.label_inspect_mode)
        }
    }

    private fun selectVisaCardProfile() {
        arrayItemDialog(
            context = requireContext(),
            items = arrayOf("Visa CVN 10", "Visa CVN 17", "Visa CVN 18"),
            title = getString(R.string.label_select_card),
        ) { selected ->
            val cardProfilePath = when (selected) {
                0 -> "${assetsPathCardVisa}_cvn10.json"
                1 -> "${assetsPathCardVisa}_cvn17.json"
                else -> "${assetsPathCardVisa}_cvn18.json"
            }
            val cardProfile = AssetsUtil.readFile<CardProfile>(requireContext().applicationContext, cardProfilePath)
            PreferencesUtil.saveCardProfile(requireContext().applicationContext, cardProfile, PaymentMethod.VISA)
        }
    }

    private fun emvKernel() {
        viewModel.prepareCardReader(requireContext().applicationContext, requireActivity())
        binding.tvPrompt.visibility = View.VISIBLE

        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Authorised amount")
        viewModel.setInputData1Filter(12, inputFormat = DataFormat.DECIMAL)

        binding.tilData2.visibility = View.VISIBLE
        viewModel.inputData2Label.set("Cashback amount")
        viewModel.setInputData2Filter(12, inputFormat = DataFormat.DECIMAL)

        binding.ivSetting.visibility = View.VISIBLE
        binding.ivSetting.setOnClickListener {
            val emvConfig = PreferencesUtil.getEmvConfig(requireContext().applicationContext)
            editConfigJson(requireContext(), it, emvConfig, true) { editedConfig ->
                PreferencesUtil.saveEmvConfig(requireContext().applicationContext, editedConfig)
            }
        }

        binding.opt1CheckBox.visibility = View.VISIBLE
        binding.opt1CheckBox.text = getString(R.string.label_inspect_mode)

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_start)
        binding.operationBtn1.isEnabled = true
        binding.operationBtn1.setOnClickListener {
            val authorizedAmount = viewModel.inputData1.get().let { if (!it.isNullOrEmpty()) it else "100" }.padStart(12, '0')
            val cashbackAmount = (viewModel.inputData2.get() ?: "00").padStart(12, '0')
            viewModel.inputData1.set(authorizedAmount)
            viewModel.inputData2.set(cashbackAmount)
            val emvConfig = PreferencesUtil.getEmvConfig(requireContext().applicationContext)
            viewModel.cardReader?.startEMV(authorizedAmount, cashbackAmount, emvConfig)
        }

        binding.operationBtn2.visibility = View.VISIBLE
        binding.operationBtn2.text = getString(R.string.label_operation_abort)
        binding.operationBtn2.isEnabled = false
        binding.operationBtn2.setOnClickListener {
            viewModel.cardReader?.status?.postValue(BasicCardReader.Companion.CardReaderStatus.ABORT)
            viewModel.cardReader?.disconnect()
        }

        EMVKernel.apdu.observe(viewLifecycleOwner) { apdu ->
            Log.d("emvKernel", "apdu: $apdu")
            if (!binding.opt1CheckBox.isChecked) {
                apdu?.let { coreViewModel.printLog(it) }
            } else {
                apdu?.let {
                    viewModel.getInspectLog(it)
                }?.apply {
                    coreViewModel.printLog(this)
                }
            }
        }

        viewModel.cardReader?.status?.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.promptMessage.set(it.name)
                when (it) {
                    BasicCardReader.Companion.CardReaderStatus.READY -> {
                        binding.operationBtn1.isEnabled = false
                        binding.operationBtn2.isEnabled = true
                    }

                    else -> {
                        binding.operationBtn1.isEnabled = true
                        binding.operationBtn2.isEnabled = false
                    }
                }
            }
        }
    }

    private fun arqcCalculator() {
        binding.arqcContainer.visibility = View.VISIBLE
        binding.tilCondition1.hint = "Card Type"
        val cardTypeList = listOf(
            PaymentMethod.VISA,
            PaymentMethod.MASTER,
            PaymentMethod.UNIONPAY,
            PaymentMethod.JCB,
            PaymentMethod.DISCOVER,
            PaymentMethod.AMEX,
        )
        binding.autoTvCondition1.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                cardTypeList,
            )
        )
        binding.autoTvCondition1.setText(cardTypeList.first().name)
        var data = hashMapOf<String, String>()
        val tagList = "9F029F039F1A955F2A9A9C9F37829F369F10575F34"
        TlvUtil.readTagList(tagList).forEach {
            data[it] = ""
        }

        binding.operationBtn3.visibility = View.VISIBLE
        binding.operationBtn3.text = getString(R.string.label_data_object_list)
        binding.operationBtn3.isEnabled = true
        binding.operationBtn3.setOnClickListener {
            editConfigJson(requireContext(), it, data, true, enableSaveLoadButton = false) { editedDOL ->
                Log.d("arqcCalculator", "editedDOL: $editedDOL")
                data = editedDOL
            }
        }

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_compute)
        binding.operationBtn1.isEnabled = true
        binding.operationBtn1.setOnClickListener {
            val cardType = binding.autoTvCondition1.text.toString().toDataClass<PaymentMethod>()
            Log.d("arqcCalculator", "cardType: $cardType")
            when (cardType) {
                PaymentMethod.VISA -> {}
                PaymentMethod.MASTER -> {}
                PaymentMethod.UNIONPAY -> {}
                PaymentMethod.JCB -> {}
                PaymentMethod.DISCOVER -> {}
                PaymentMethod.AMEX -> {}
                else -> {}
            }
        }

    }

    private fun resetLayout() {
        viewModel.finishCardReader()
        binding.tvPrompt.visibility = View.GONE
        viewModel.promptMessage.set("")

        binding.ivSetting.visibility = View.GONE
        binding.ivSetting.setOnClickListener(null)

        binding.operationBtn1.visibility = View.GONE
        binding.operationBtn1.setOnClickListener(null)
        binding.operationBtn1.isEnabled = false
        binding.operationBtn2.visibility = View.GONE
        binding.operationBtn2.setOnClickListener(null)
        binding.operationBtn2.isEnabled = false
        binding.operationBtn3.visibility = View.GONE
        binding.operationBtn3.setOnClickListener(null)
        binding.operationBtn3.isEnabled = false

        binding.cardContainer.visibility = View.GONE

        binding.opt1CheckBox.visibility = View.GONE
        binding.opt1CheckBox.text = ""
        binding.opt1CheckBox.isChecked = false

        binding.ivPaymentMethod.setOnClickListener(null)
        binding.ivCard.setOnClickListener(null)

        binding.tilData1.visibility = View.GONE
        viewModel.setInputData1Filter()
        viewModel.inputData1Label.set("")

        binding.tilData2.visibility = View.GONE
        viewModel.setInputData2Filter()
        viewModel.inputData2Label.set("")

        CreditCardSimulator.apdu.removeObservers(viewLifecycleOwner)
        EMVKernel.apdu.removeObservers(viewLifecycleOwner)
        viewModel.cardReader?.status?.removeObservers(viewLifecycleOwner)

        binding.arqcContainer.visibility = View.GONE
        binding.autoTvCondition1.onItemClickListener = null
    }

    override fun onResume() {
        super.onResume()
        if (coreViewModel.currentTool.value == Tool.CARD_SIMULATOR) {
            CreditCardSimulator.enablePaymentService(requireContext().applicationContext, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("EmvFragment", "onDestroy")
        CreditCardSimulator.enablePaymentService(requireContext().applicationContext, false)
        viewModel.finishCardReader()
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