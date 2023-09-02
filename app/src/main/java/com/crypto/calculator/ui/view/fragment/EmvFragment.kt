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
import com.crypto.calculator.extension.applyPadding
import com.crypto.calculator.extension.getColorIconResId
import com.crypto.calculator.extension.requireDefaultPaymentServicePermission
import com.crypto.calculator.extension.toDataClass
import com.crypto.calculator.model.AcDOL
import com.crypto.calculator.model.CryptogramKey
import com.crypto.calculator.model.DataFormat
import com.crypto.calculator.model.EMVPublicKey
import com.crypto.calculator.model.OdaDOL
import com.crypto.calculator.model.PaddingMethod
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.model.Tool
import com.crypto.calculator.service.cardSimulator.CreditCardService
import com.crypto.calculator.service.model.CardProfile
import com.crypto.calculator.ui.base.BaseActivity
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewAdapter.DropDownMenuAdapter
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.ui.viewModel.EmvViewModel
import com.crypto.calculator.util.AssetsUtil
import com.crypto.calculator.util.EMVUtils
import com.crypto.calculator.util.Encryption
import com.crypto.calculator.util.LogPanelUtil
import com.crypto.calculator.util.ODAUtil
import com.crypto.calculator.util.PreferencesUtil
import com.crypto.calculator.util.TlvUtil
import com.crypto.calculator.util.assetsPathCardMaster
import com.crypto.calculator.util.assetsPathCardVisa
import com.crypto.calculator.util.bindInputFilters
import com.crypto.calculator.util.prefCardProfile
import com.crypto.calculator.util.prefEmvConfig
import com.google.android.material.snackbar.Snackbar

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
            CreditCardService.enablePaymentService(requireContext().applicationContext, false)
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
            Tool.ODA -> odaCalculator()
            else -> {}
        }
    }

    private fun cardSimulator() {
        CreditCardService.enablePaymentService(requireContext().applicationContext, true)
        binding.tvPrompt.visibility = View.VISIBLE

        (requireActivity() as BaseActivity).requireDefaultPaymentServicePermission {
            binding.cardContainer.visibility = View.VISIBLE
            viewModel.promptMessage.set(getString(R.string.label_present_card_to_reader))
            viewModel.cardPreference.observe(viewLifecycleOwner) {
                binding.ivPaymentMethod.setImageResource(it.getColorIconResId())
            }

            CreditCardService.apdu.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.also { apdu ->
                    Log.d("cardSimulator", "apdu: $apdu")
                    if (!binding.opt1CheckBox.isChecked) {
                        LogPanelUtil.printLog(apdu)
                    } else {
                        LogPanelUtil.printLog(viewModel.getInspectLog(requireContext().applicationContext, apdu))
                    }
                }
            }

            CreditCardService.status.observe(viewLifecycleOwner) {
                when (it) {
                    CreditCardService.Companion.CardSimulatorStatus.PROCESSING -> {}

                    else -> {
                        Log.d("CardSimulatorStatus observe", "transactionData: ${viewModel.currentTransactionData}")
                        viewModel.resetTransactionData()
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
                editConfigJson(
                    view = binding.root,
                    context = requireContext(),
                    config = PreferencesUtil.getCardProfile(requireContext().applicationContext, cardPreference),
                    onConfirmClick = {
                        PreferencesUtil.saveCardProfile(requireContext().applicationContext, it, cardPreference)
                    },
                    neutralBtn = getString(R.string.button_reset),
                    onNeutralBtnClick = {
                        PreferencesUtil.clearPreferenceData(requireContext().applicationContext, "${cardPreference}-$prefCardProfile")
                    }
                )

            }

            binding.ivCard.setOnLongClickListener {
                when (PreferencesUtil.getCardPreference(requireContext().applicationContext)) {
                    PaymentMethod.VISA -> selectVisaCardProfile()
                    PaymentMethod.MASTER -> selectMasterCardProfile()
                    else -> {}
                }
                true
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

    private fun selectMasterCardProfile() {
        arrayItemDialog(
            context = requireContext(),
            items = arrayOf("Without PIN", "With PIN"),
            title = getString(R.string.label_select_card),
        ) { selected ->
            val cardProfilePath = when (selected) {
                0 -> "${assetsPathCardMaster}.json"
                else -> "${assetsPathCardMaster}_pin.json"
            }
            val cardProfile = AssetsUtil.readFile<CardProfile>(requireContext().applicationContext, cardProfilePath)
            PreferencesUtil.saveCardProfile(requireContext().applicationContext, cardProfile, PaymentMethod.MASTER)
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
            editConfigJson(requireContext(), it, emvConfig, true,
                neutralBtn = getString(R.string.button_reset),
                onNeutralBtnClick = {
                    PreferencesUtil.clearPreferenceData(requireContext().applicationContext, prefEmvConfig)
                }
            ) { editedConfig ->
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

        EMVKernel.apdu.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { apdu ->
                Log.d("emvKernel", "apdu: $apdu")
                if (!binding.opt1CheckBox.isChecked) {
                    LogPanelUtil.printLog(apdu)
                } else {
                    LogPanelUtil.printLog(viewModel.getInspectLog(requireContext().applicationContext, apdu))
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

                    BasicCardReader.Companion.CardReaderStatus.FAIL,
                    BasicCardReader.Companion.CardReaderStatus.ABORT,
                    BasicCardReader.Companion.CardReaderStatus.SUCCESS -> {
                        viewModel.resetTransactionData()
                        viewModel.cardReader?.disconnect()
                        binding.operationBtn1.isEnabled = true
                        binding.operationBtn2.isEnabled = false
                    }

                    else -> {}
                }
            }
        }
    }

    private fun arqcCalculator() {
        fun initData(tagList: String = "9F029F039F1A955F2A9A9C9F37829F369F10"): HashMap<String, String> {
            val sb = StringBuilder()
            sb.append(tagList)
            sb.append("57")
            sb.append("5F34")
            val data = hashMapOf<String, String>()
            TlvUtil.readTagList(sb.toString()).forEach {
                data[it] = ""
            }
            return data
        }

        binding.tilData1.visibility = View.VISIBLE
        viewModel.inputData1Label.set("Issuer Application Data [9F10]")
        viewModel.setInputData1Filter(inputFormat = DataFormat.HEXADECIMAL)

        var data = hashMapOf<String, String>()
        var tagList = ""

        binding.tilCondition1.visibility = View.VISIBLE
        binding.operationBtn3.visibility = View.VISIBLE

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
//        binding.autoTvCondition1.setText(cardTypeList.first().name)
        binding.autoTvCondition1.setOnItemClickListener { _, _, _, _ ->
            try {
                val cardType = binding.autoTvCondition1.text.toString().toDataClass<PaymentMethod>()
                val cvn = viewModel.inputData1.get()?.let { iad -> EMVUtils.getCVNByPaymentMethod(cardType, iad) }
                Log.d("arqcCalculator", "cvn: $cvn")
                tagList = EMVUtils.getAcTagListByPaymentMethod(cardType, cvn)
                data = initData(tagList)
            } catch (ex: Exception) {
                Log.d("arqcCalculator", "Invalid IAD [9F10]")
                data = hashMapOf()
                Snackbar.make(binding.root, "Invalid IAD [9F10]", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.operationBtn3.visibility = View.VISIBLE
        binding.operationBtn3.text = getString(R.string.label_data_object_list)
        binding.operationBtn3.setOnClickListener {
            editConfigJson(requireContext(), it, AcDOL(data), true,
                neutralBtn = getString(R.string.button_reset),
                onNeutralBtnClick = {
                    data = initData(tagList)
                }
            ) { editedDOL ->
                Log.d("arqcCalculator", "editedDOL: $editedDOL")
                data = editedDOL.data
            }
        }

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_compute)
        binding.operationBtn1.isEnabled = true
        binding.operationBtn1.setOnClickListener {
            val cardType = binding.autoTvCondition1.text.toString().toDataClass<PaymentMethod>()
            Log.d("arqcCalculator", "cardType: $cardType")
            val iad = LogPanelUtil.safeExecute { data["9F10"] ?: "" }
            val cvn = LogPanelUtil.safeExecute(onFail = {}, task = { EMVUtils.getCVNByPaymentMethod(cardType, iad) })
            val imk = LogPanelUtil.safeExecute { EMVUtils.getIssuerMasterKeyByPaymentMethod(requireContext().applicationContext, cardType) ?: "Issuer master key not found" }
            val pan = LogPanelUtil.safeExecute { data["57"]?.substringBefore('D') ?: "" }
            val psn = LogPanelUtil.safeExecute { data["5F34"] ?: "" }

            val iccMK = LogPanelUtil.safeExecute { EMVUtils.deriveICCMasterKey(imk, pan, psn) }
            val atc = LogPanelUtil.safeExecute { data["9F36"] ?: "" }
            val un = LogPanelUtil.safeExecute { data["9F37"] ?: "" }
            val udk = LogPanelUtil.safeExecute { EMVUtils.deriveACSessionKey(cardType, iccMK, atc, un) }
            Log.d("arqcCalculator", "udk: $udk")

            val acCalculationKey = LogPanelUtil.safeExecute(onFail = null, task = { EMVUtils.getACCalculationKey(cardType, cvn) }) ?: CryptogramKey.ICC_MASTER_KEY
            Log.d("arqcCalculator", "AC calculation key: $acCalculationKey")

            val key = when (acCalculationKey) {
                CryptogramKey.SESSION_KEY -> udk
                CryptogramKey.ICC_MASTER_KEY -> iccMK
            }
            val dol = LogPanelUtil.safeExecute { EMVUtils.getAcDOLByPaymentMethod(cardType, cvn, data) }
            Log.d("arqcCalculator", "dol: $dol")
            val paddingMethod = LogPanelUtil.safeExecute(onFail = {}, task = { EMVUtils.getAcDOLPaddingByPaymentMethod(cardType, cvn) }) ?: PaddingMethod.ISO9797_M1
            Log.d("arqcCalculator", "paddingMethod: $paddingMethod")
            val arqc = LogPanelUtil.safeExecute { Encryption.calculateMAC(key, dol.applyPadding(paddingMethod)).uppercase() }
            Log.d("arqcCalculator", "arqc: $arqc")

            LogPanelUtil.printLog(
                "ARQC_CALCULATOR: " +
                        "\nIssuer master key: $imk " +
                        (if (pan.isNotEmpty()) "\nPAN [5A]: $pan " else "") +
                        (if (psn.isNotEmpty()) "\nPSN [5F34]: $psn " else "") +
                        "\nICC master key: $iccMK " +
                        (if (atc.isNotEmpty() && acCalculationKey == CryptogramKey.SESSION_KEY) "\nATC [9F36]: $atc " else "") +
                        (if (un.isNotEmpty() && acCalculationKey == CryptogramKey.SESSION_KEY && cardType == PaymentMethod.MASTER) "\nUN [9F37]: $un " else "") +
                        (if (key == udk) "\nAC session key: $udk " else "") +
                        "\nDOL: $dol " +
                        "\nPadding method: ${paddingMethod.name} " +
                        "\nARQC: $arqc\n"
            )
        }
    }

    private fun odaCalculator() {
        fun initData(selected: Int?): HashMap<String, String> {
            val sb = StringBuilder()
            when (selected) {
                0 -> sb.append("8F90929F32")
                1 -> sb.append("9F469F479F48")
                2 -> sb.append("93")
                3 -> sb.append("9F4B")
                else -> {}
            }
            val data = hashMapOf<String, String>()
            TlvUtil.readTagList(sb.toString()).forEach {
                data[it] = ""
            }
            when (selected) {
                1, 2 -> {
                    data["issuerPublicKeyExponent"] = ""
                    data["issuerPublicKeyModulus"] = ""
                    data["staticData"] = ""
                }

                3 -> {
                    data["iccPublicKeyExponent"] = ""
                    data["iccPublicKeyModulus"] = ""
                    data["dynamicData"] = ""
                }

                else -> {}
            }
            return data
        }

        var data = hashMapOf<String, String>()
        var selected: Int? = null
        val operationList = listOf("Retrieve Issuer Public Key", "Retrieve ICC Public Key", "Verify Signed Static Application Data", "Verify Signed Dynamic Application Data")
        binding.tilCondition2.visibility = View.VISIBLE
        binding.tilCondition2.hint = "Operation"
        binding.autoTvCondition2.setAdapter(
            DropDownMenuAdapter(
                requireContext(),
                R.layout.view_drop_down_menu_item,
                operationList,
            )
        )
        binding.autoTvCondition2.setOnItemClickListener { _, _, i, _ ->
            selected = i
            data = initData(selected)
        }

        binding.operationBtn4.visibility = View.VISIBLE
        binding.operationBtn4.text = getString(R.string.label_data_object_list)
        binding.operationBtn4.setOnClickListener {
            editConfigJson(requireContext(), it, OdaDOL(data), true,
                neutralBtn = getString(R.string.button_reset),
                onNeutralBtnClick = {
                    data = initData(selected)
                }
            ) { editedDOL ->
                Log.d("arqcCalculator", "editedDOL: $editedDOL")
                data = editedDOL.data
            }
        }

        binding.operationBtn1.visibility = View.VISIBLE
        binding.operationBtn1.text = getString(R.string.label_operation_compute)
        binding.operationBtn1.isEnabled = true
        binding.operationBtn1.setOnClickListener {
            val logBuilder = StringBuilder()
            when (selected) {
                0 -> {
                    logBuilder.append("ODA calculator - ${operationList[0]}:\n")
                    val capk = data["8F"]?.let { capkIdx ->
                        logBuilder.append("CAPK index [8F]: $capkIdx\n")
                        val capkList = PreferencesUtil.getCapkData(requireContext().applicationContext)
                        capkList.data?.find { it.index == capkIdx }
                    }
                    capk?.also {
                        it.exponent.also { e -> logBuilder.append("CAPK exponent: $e\n") }
                        it.modulus.also { modulus -> logBuilder.append("CAPK modulus: $modulus\n") }
                        val issuerPKCert = data["90"]?.also { cert -> logBuilder.append("Issuer Public Key Certificate [90]: $cert\n") } ?: ""
                        data["92"]?.also { remainder -> if (remainder.isNotEmpty()) logBuilder.append("Issuer Public Key Remainder [92]: $remainder\n") }

                        val recoveredData = LogPanelUtil.safeExecute { Encryption.doRSA(issuerPKCert, capk.exponent, capk.modulus) }
                        logBuilder.append("Recovered Data: $recoveredData\n")
                        val inspectLog = LogPanelUtil.safeExecute { viewModel.inspectIssuerPKPlainCert(recoveredData, data) }
                        logBuilder.append("$inspectLog\n")
                    } ?: run {
                        logBuilder.append("Invalid ICC data [8F]\n")
                    }

                    val issuerPK = LogPanelUtil.safeExecute(onFail = { logBuilder.append("Error: ${it.message ?: it.toString()}\n") },
                        task = { ODAUtil.retrieveIssuerPK(requireContext().applicationContext, data) })
                    Log.d("odaCalculator", "result: $issuerPK")
                    issuerPK?.also {
                        logBuilder.append("Result: Issuer Public Key\n")
                        it.exponent?.also { e -> logBuilder.append("exponent: $e\n") }
                        it.modulus?.also { modulus -> logBuilder.append("modulus: $modulus\n") }
                    }
                    LogPanelUtil.printLog(logBuilder.toString())
                }

                1 -> {
                    logBuilder.append("ODA calculator - ${operationList[1]}:\n")
                    val issuerPK = EMVPublicKey(
                        exponent = data["issuerPublicKeyExponent"],
                        modulus = data["issuerPublicKeyModulus"]
                    )
                    issuerPK.exponent.also { e -> logBuilder.append("Issuer Public Key exponent: $e\n") }
                    issuerPK.modulus.also { modulus -> logBuilder.append("Issuer Public Key modulus: $modulus\n") }
                    Log.d("odaCalculator", "issuerPK: $issuerPK")

                    val staticData = data["staticData"] ?: ""
                    Log.d("odaCalculator", "staticData: $staticData")
                    logBuilder.append("Static Data: $staticData\n")

                    val iccPKCert = data["9F46"]?.also { cert -> logBuilder.append("ICC Public Key Certificate [9F46]: $cert\n") } ?: ""
                    data["9F48"]?.also { remainder -> if (remainder.isNotEmpty()) logBuilder.append("ICC Public Key Remainder [9F48]: $remainder\n") }
                    val recoveredData = LogPanelUtil.safeExecute { Encryption.doRSA(iccPKCert, issuerPK.exponent!!, issuerPK.modulus!!) }
                    logBuilder.append("Recovered Data: $recoveredData\n")
                    val inspectLog = LogPanelUtil.safeExecute { viewModel.inspectIccPKPlainCert(recoveredData, data) }
                    logBuilder.append("$inspectLog\n")

                    val iccPK = LogPanelUtil.safeExecute(onFail = { logBuilder.append("Error: ${it.message ?: it.toString()}\n") },
                        task = { ODAUtil.retrieveIccPK(staticData, data, issuerPK) })
                    Log.d("odaCalculator", "iccPK: $iccPK")
                    iccPK?.also {
                        logBuilder.append("Result: ICC Public Key\n")
                        it.exponent?.also { e -> logBuilder.append("exponent: $e\n") }
                        it.modulus?.also { modulus -> logBuilder.append("modulus: $modulus\n") }
                    }
                    LogPanelUtil.printLog(logBuilder.toString())
                }

                2 -> {
                    logBuilder.append("ODA calculator - ${operationList[2]}:\n")
                    val issuerPK = EMVPublicKey(
                        exponent = data["issuerPublicKeyExponent"],
                        modulus = data["issuerPublicKeyModulus"]
                    )
                    issuerPK.exponent.also { e -> logBuilder.append("Issuer Public Key exponent: $e\n") }
                    issuerPK.modulus.also { modulus -> logBuilder.append("Issuer Public Key modulus: $modulus\n") }
                    Log.d("odaCalculator", "issuerPK: $issuerPK")

                    val staticData = data["staticData"] ?: ""
                    Log.d("odaCalculator", "staticData: $staticData")
                    logBuilder.append("Static Data: $staticData\n")

                    val ssad = data["93"] ?: ""
                    logBuilder.append("Signed Static Application Data: $ssad\n")
                    val recoveredData = LogPanelUtil.safeExecute { Encryption.doRSA(ssad, issuerPK.exponent!!, issuerPK.modulus!!) }
                    logBuilder.append("Recovered Data: $recoveredData\n")
                    val inspectLog = LogPanelUtil.safeExecute { viewModel.inspectSignedStaticApplicationData(recoveredData, data) }
                    logBuilder.append("$inspectLog\n")

                    val result = LogPanelUtil.safeExecute(
                        onFail = { logBuilder.append("Error: ${it.message ?: it.toString()}\n") },
                        task = { ODAUtil.verifySSAD(recoveredData, staticData, data) }
                    )
                    if (result == true) {
                        logBuilder.append("✓ Signed Static Application Data Validation Succeed\n")
                    } else {
                        logBuilder.append("✗ Signed Static Application Data Validation Failed\n")
                    }
                    LogPanelUtil.printLog(logBuilder.toString())
                }

                3 -> {
                    logBuilder.append("ODA calculator - ${operationList[3]}:\n")
                    val iccPK = EMVPublicKey(
                        exponent = data["iccPublicKeyExponent"],
                        modulus = data["iccPublicKeyModulus"]
                    )
                    Log.d("odaCalculator", "iccPK: $iccPK")
                    iccPK.exponent.also { e -> logBuilder.append("ICC Public Key exponent: $e\n") }
                    iccPK.modulus.also { modulus -> logBuilder.append("ICC Public Key modulus: $modulus\n") }

                    val staticData = data["dynamicData"] ?: ""
                    Log.d("odaCalculator", "dynamicData: $staticData")
                    logBuilder.append("Dynamic Data: $staticData\n")

                    val sdad = data["9F4B"] ?: ""
                    logBuilder.append("Signed Dynamic Application Data: $sdad\n")
                    val recoveredData = LogPanelUtil.safeExecute { Encryption.doRSA(sdad, iccPK.exponent!!, iccPK.modulus!!) }
                    logBuilder.append("Recovered Data: $recoveredData\n")
                    val inspectLog = LogPanelUtil.safeExecute { viewModel.inspectSignedDynamicApplicationData(recoveredData, data) }
                    logBuilder.append("$inspectLog\n")

                    val result = LogPanelUtil.safeExecute(
                        onFail = { logBuilder.append("Error: ${it.message ?: it.toString()}\n") },
                        task = { ODAUtil.verifySDAD(recoveredData, staticData) }
                    )
                    if (result == true) {
                        logBuilder.append("✓ Signed Dynamic Application Data Validation Succeed\n")
                    } else {
                        logBuilder.append("✗ Signed Dynamic Application Data Validation Failed\n")
                    }
                    LogPanelUtil.printLog(logBuilder.toString())
                }

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
        binding.operationBtn4.visibility = View.GONE
        binding.operationBtn4.setOnClickListener(null)
        binding.operationBtn4.isEnabled = false

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

        CreditCardService.apdu.removeObservers(viewLifecycleOwner)
        EMVKernel.apdu.removeObservers(viewLifecycleOwner)
        viewModel.cardReader?.status?.removeObservers(viewLifecycleOwner)

        binding.tilCondition1.visibility = View.GONE
        binding.autoTvCondition1.onItemClickListener = null
        binding.autoTvCondition1.text.clear()

        binding.tilCondition2.visibility = View.GONE
        binding.autoTvCondition2.onItemClickListener = null
        binding.autoTvCondition2.text.clear()

        viewModel.resetTransactionData()
    }

    override fun onResume() {
        super.onResume()
        if (coreViewModel.currentTool.value == Tool.CARD_SIMULATOR) {
            CreditCardService.enablePaymentService(requireContext().applicationContext, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("EmvFragment", "onDestroy")
        CreditCardService.enablePaymentService(requireContext().applicationContext, false)
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