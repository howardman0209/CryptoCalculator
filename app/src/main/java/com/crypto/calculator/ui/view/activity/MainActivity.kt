package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.MainApplication
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.model.NavigationMenuData
import com.crypto.calculator.model.Tool
import com.crypto.calculator.model.getGroupList
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.view.fragment.CoreFragment
import com.crypto.calculator.ui.viewAdapter.ExpandableMenuAdapter
import com.crypto.calculator.ui.viewModel.MainViewModel
import com.crypto.calculator.util.PreferencesUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : MVVMActivity<MainViewModel, ActivityMainBinding>() {
    private lateinit var mainFragment: Fragment
    private lateinit var menuAdapter: ExpandableMenuAdapter
    private lateinit var navigationMenuData: NavigationMenuData
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.topAppBar)

        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        navigationMenuData = MainApplication.getNavigationMenuData()

        menuAdapter = ExpandableMenuAdapter(
            this,
            navigationMenuData.getGroupList(),
            navigationMenuData.data
        )
        binding.expandableMenu.setAdapter(menuAdapter)

        binding.expandableMenu.setOnChildClickListener { expandableListView, view, groupPosition, childPosition, l ->
            val selectedGroup = navigationMenuData.getGroupList()[groupPosition]
            Log.d("expandableMenu", "Selected group: $selectedGroup, child: $childPosition")

            switchTool(groupPosition, childPosition)
            navigationMenuData.data[selectedGroup]?.get(childPosition)?.also {
                setAppbarTitle(it)
            }
            binding.drawerLayout.close()
            false
        }

        mainFragment = CoreFragment()
        switchTool()
        setAppbarTitle(PreferencesUtil.getLastUsedTool(applicationContext))
    }

    private fun setAppbarTitle(tool: Tool = Tool.TLV_PARSER) {
        val title = getString(tool.resourceId)
        viewModel.title.set(title)
    }

    private fun switchTool(groupId: Int? = null, itemId: Int? = null) {
        navigationMenuData.also {
            if (groupId != null && itemId != null) {
                val group = it.getGroupList()[groupId]
                it.data[group]?.get(itemId)?.let { selectedTool ->
                    (mainFragment as CoreFragment).setCurrentTool(selectedTool)
                    PreferencesUtil.saveLastUsedTool(applicationContext, selectedTool)
                }
            } else {
                // Expand Generic tab by default
                val lastUsedParentGroup = it.getGroupList().find { group -> it.data[group]?.contains(PreferencesUtil.getLastUsedTool(applicationContext)) == true }
                binding.expandableMenu.expandGroup(it.getGroupList().indexOf(lastUsedParentGroup))
            }
            pushFragment(mainFragment, getMainFragmentContainer(), isAddToBackStack = false)
        }
    }

    private fun backToLogin() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.label_logout)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.button_cancel)
            { dialog, _ ->
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