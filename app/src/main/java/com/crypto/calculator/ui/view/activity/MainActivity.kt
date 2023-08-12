package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.viewModel.MainViewModel

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private var currentBottomPanel: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            Log.d("navigationView", "Selected: $menuItem")
            when (menuItem.itemId) {
                R.id.nav_home -> Log.d("navigationView", "go to home fragment")
            }
            menuItem.isChecked = true
            binding.drawerLayout.close()
            true
        }

//        appendLog("Hello World")
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