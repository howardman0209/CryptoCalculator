package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.view.fragment.CoreFragment
import com.crypto.calculator.ui.viewModel.MainViewModel

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private var currentBottomPanel: Fragment? = null
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

            menuItem.isChecked = true
            binding.drawerLayout.close()
            true
        }

        switchTab()
    }

    private fun switchTab(itemId: Int = 0) {
        val fragment = when (itemId) {
            0 -> {
                binding.navigationView.setCheckedItem(R.id.nav_tab1)
                CoreFragment()
            }

            R.id.nav_tab1 -> CoreFragment()
            else -> CoreFragment()
        }
        pushFragment(fragment, getMainFragmentContainer(), isAddToBackStack = false)
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