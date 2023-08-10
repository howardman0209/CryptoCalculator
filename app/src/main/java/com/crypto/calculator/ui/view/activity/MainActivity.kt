package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.view.fragment.OutputFragment
import com.crypto.calculator.ui.viewModel.MainViewModel

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            Log.d("MainActivity", "NavigationOnClick")
        }

        binding.buttonMore.setOnClickListener {

        }

        switchPanel()

//        appendLog("Hello World")
    }

    private fun switchPanel(itemId: Int? = null, target: Int = R.id.bottomPanel) {
        val fragment = OutputFragment()
//            when (itemId) {
//                R.id.btnHome -> QuickLaunchFragment()
//                R.id.btnSupport -> SupportFragment()
//                R.id.btnMore -> MoreFragment()
//                R.id.btnPromotion -> PromotionsFragment()
//                else -> QuickLaunchFragment()
//            }
        pushFragment(fragment, target, isAddToBackStack = false)
    }


    override fun getViewModelInstance(): MainViewModel {
        return ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_main
}