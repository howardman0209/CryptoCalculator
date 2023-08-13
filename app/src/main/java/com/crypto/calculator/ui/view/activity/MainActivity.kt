package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.model.Tools
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.view.fragment.CoreFragment
import com.crypto.calculator.ui.viewModel.MainViewModel

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private var mainFragment = CoreFragment()
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

        switchTab()
        setAppbarTitle()
    }

    private fun setAppbarTitle(title: String = getString(R.string.label_tool_tlv_parser)) {
        viewModel.title.set(title)
    }

    private fun switchTab(itemId: Int = 0) {
        when (itemId) {
            R.id.nav_tab1 -> mainFragment.setCorePanel(Tools.TLV_PARSER)
            R.id.nav_tab2 -> mainFragment.setCorePanel(Tools.DES)
            R.id.nav_tab3 -> mainFragment.setCorePanel(Tools.HASH)
            else -> {
                mainFragment.setCorePanel(Tools.TLV_PARSER)
                binding.navigationView.setCheckedItem(R.id.nav_tab1)
            }
        }
        pushFragment(mainFragment, getMainFragmentContainer(), isAddToBackStack = false)
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
}