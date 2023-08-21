package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentEmvBinding
import com.crypto.calculator.extension.getColorIconResId
import com.crypto.calculator.extension.requireDefaultPaymentServicePermission
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.model.Tool
import com.crypto.calculator.service.cardSimulator.CreditCardSimulator
import com.crypto.calculator.service.model.CardProfile
import com.crypto.calculator.ui.base.BaseActivity
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.ui.viewModel.EmvViewModel
import com.crypto.calculator.util.AssetsUtil
import com.crypto.calculator.util.PreferencesUtil
import com.crypto.calculator.util.assetsPathCardVisa

class EmvFragment : MVVMFragment<EmvViewModel, FragmentEmvBinding>() {
    private lateinit var coreViewModel: CoreViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coreViewModel = getCoreViewModel()
        viewModel.cardPreference.value = PreferencesUtil.getCardPreference(requireContext().applicationContext)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EmvFragment", "onViewCreated")

        coreViewModel.currentTool.observe(viewLifecycleOwner) {
            Log.d("EmvFragment", "currentTool: $it")
            CreditCardSimulator.enablePaymentService(requireContext().applicationContext, false)
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
        CreditCardSimulator.apdu.removeObservers(viewLifecycleOwner)
    }

    private fun cardSimulator() {
        CreditCardSimulator.enablePaymentService(requireContext().applicationContext, true)
        (requireActivity() as BaseActivity).requireDefaultPaymentServicePermission {
            binding.cardContainer.visibility = View.VISIBLE
            viewModel.promptMessage.set(getString(R.string.label_present_card_to_reader))
            viewModel.cardPreference.observe(viewLifecycleOwner) {
                binding.ivPaymentMethod.setImageResource(it.getColorIconResId())
            }

            CreditCardSimulator.apdu.observe(viewLifecycleOwner) { apdu ->
                Log.d("EmvFragment", "apdu: $apdu")
                apdu?.let { coreViewModel.printLog(it) }
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
        binding.animationAwaitCard.visibility = View.VISIBLE
        viewModel.promptMessage.set(getString(R.string.emv_label_tap))
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