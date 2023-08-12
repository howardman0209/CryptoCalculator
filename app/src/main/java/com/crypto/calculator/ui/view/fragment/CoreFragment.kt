package com.crypto.calculator.ui.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentCoreBinding
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.CoreViewModel

class CoreFragment : MVVMFragment<CoreViewModel, FragmentCoreBinding>() {
    private var currentBottomPanel: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        binding.toolbar.setNavigationOnClickListener {
//            Log.d("MainActivity", "NavigationOnClick")
//        }
//
//        binding.buttonMore.setOnClickListener {
//            viewModel.printLog("Hello World")
//        }

        switchPanel()

//        appendLog("Hello World")
    }

    private fun switchPanel(itemId: Int? = null, target: Int = R.id.bottomPanel) {
        currentBottomPanel = OutputFragment()
//            when (itemId) {
//                R.id.btnHome -> QuickLaunchFragment()
//                R.id.btnSupport -> SupportFragment()
//                R.id.btnMore -> MoreFragment()
//                R.id.btnPromotion -> PromotionsFragment()
//                else -> QuickLaunchFragment()
//            }
//        pushFragment(currentBottomPanel as Fragment, target, isAddToBackStack = false)
    }


    override fun getViewModelInstance(): CoreViewModel {
        return ViewModelProvider(this)[CoreViewModel::class.java]
    }

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.fragment_core
    override fun screenName(): String = "CoreFragment"
}