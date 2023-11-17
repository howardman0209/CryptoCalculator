package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
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
    private var currentTools: Tool? = null
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

            navigationMenuData.data[selectedGroup]?.get(childPosition)?.also { selectedTool ->
                currentTools = selectedTool
                PreferencesUtil.saveLastUsedTool(applicationContext, selectedTool)
                setAppbarTitle(selectedTool)
                binding.drawerLayout.close()
            }
            false
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                currentTools?.let { (mainFragment as CoreFragment).setCurrentTool(it) }
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })

        mainFragment = CoreFragment()
        pushFragment(mainFragment, getMainFragmentContainer(), isAddToBackStack = false)

        // Push to last used tool and expand corresponding tab by default
        PreferencesUtil.getLastUsedTool(applicationContext).also { lastUsedTool ->
            navigationMenuData.also {
                val lastUsedParentGroup = it.getGroupList().find { group -> it.data[group]?.contains(lastUsedTool) == true }
                binding.expandableMenu.expandGroup(it.getGroupList().indexOf(lastUsedParentGroup))
            }
            setAppbarTitle(lastUsedTool)
//            (mainFragment as CoreFragment).setCurrentTool(lastUsedTool)
        }
    }

    fun closeNavigationMenu() {
        binding.drawerLayout.close()
    }

    private fun setAppbarTitle(tool: Tool = Tool.TLV_PARSER) {
        val title = getString(tool.resourceId)
        viewModel.title.set(title)
    }

    private fun findTool(groupId: Int, itemId: Int): Tool? {
        navigationMenuData.also {
            val group = it.getGroupList()[groupId]
            return it.data[group]?.get(itemId)
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