package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.model.Tools
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.view.fragment.CoreFragment
import com.crypto.calculator.ui.viewModel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private lateinit var mainFragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.topAppBar)

        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            Log.d("navigationView", "Selected: $menuItem")

            switchTab(menuItem.itemId)
            setAppbarTitle(menuItem.title.toString())

            menuItem.isChecked = true
            binding.drawerLayout.close()
            true
        }
        mainFragment = CoreFragment()
        switchTab()
        setAppbarTitle()
    }

    private fun setAppbarTitle(title: String = getString(R.string.label_tool_tlv_parser)) {
        viewModel.title.set(title)
    }

    private fun switchTab(itemId: Int = 0) {
        when (itemId) {
            R.id.nav_tab1 -> (mainFragment as CoreFragment).setCorePanel(Tools.TLV_PARSER)
            R.id.nav_tab2 -> (mainFragment as CoreFragment).setCorePanel(Tools.DES)
            R.id.nav_tab3 -> (mainFragment as CoreFragment).setCorePanel(Tools.HASH)
            else -> {
                binding.navigationView.setCheckedItem(R.id.nav_tab1)
            }
        }
        pushFragment(mainFragment, getMainFragmentContainer(), isAddToBackStack = false)
    }

    private fun backToLogin() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.label_logout)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun getMainFragmentContainer(): Int = R.id.mainFragmentContainer

    override fun getViewModelInstance(): MainViewModel {
        return ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_main

    override fun onBackPressed() {
        backToLogin()
    }
}