package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.widget.ExpandableListAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivityMainBinding
import com.crypto.calculator.model.NavigationMenuData
import com.crypto.calculator.model.Tool
import com.crypto.calculator.model.getGroupList
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.view.fragment.CoreFragment
import com.crypto.calculator.ui.viewAdapter.ExpandableMenuAdapter
import com.crypto.calculator.ui.viewModel.MainViewModel
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

        navigationMenuData = getNavigationMenuData()

//        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
//            // Handle menu item selected
//            Log.d("navigationView", "Selected: $menuItem")
//
//            switchTab(menuItem.itemId)
//            setAppbarTitle(menuItem.title.toString())
//
//            menuItem.isChecked = true
//            binding.drawerLayout.close()
//            true
//        }

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
        setAppbarTitle()
    }

    private fun setAppbarTitle(tool: Tool = Tool.TLV_PARSER) {
        val title = when (tool) {
            Tool.AES -> getString(R.string.label_tool_aes)
            Tool.TLV_PARSER -> getString(R.string.label_tool_tlv_parser)
            Tool.DES -> getString(R.string.label_tool_des)
            Tool.HASH -> getString(R.string.label_tool_hash)
            Tool.BITWISE -> getString(R.string.label_tool_bitwise)
            Tool.MAC -> getString(R.string.label_tool_mac)
            Tool.CONVERTER -> getString(R.string.label_tool_converter)
            Tool.RSA -> getString(R.string.label_tool_rsa)
        }
        viewModel.title.set(title)
    }

    private fun getNavigationMenuData(): NavigationMenuData {
        return NavigationMenuData(
            data = hashMapOf(
                "Generic" to listOf(
                    Tool.TLV_PARSER,
                    Tool.DES,
                    Tool.HASH,
                    Tool.BITWISE,
                    Tool.MAC,
                    Tool.CONVERTER,
                    Tool.RSA,
                ),
                "EMV" to emptyList(),
            )
        )
    }

//    private fun switchTab(itemId: Int = 0) {
//        when (itemId) {
//            R.id.nav_tab1 -> (mainFragment as CoreFragment).setCorePanel(Tool.TLV_PARSER)
//            R.id.nav_tab2 -> (mainFragment as CoreFragment).setCorePanel(Tool.DES)
//            R.id.nav_tab3 -> (mainFragment as CoreFragment).setCorePanel(Tool.HASH)
//            R.id.nav_tab4 -> (mainFragment as CoreFragment).setCorePanel(Tool.BITWISE)
//            R.id.nav_tab5 -> (mainFragment as CoreFragment).setCorePanel(Tool.MAC)
//            R.id.nav_tab6 -> (mainFragment as CoreFragment).setCorePanel(Tool.CONVERTER)
//            R.id.nav_tab7 -> (mainFragment as CoreFragment).setCorePanel(Tool.RSA)
//            else -> {
//                binding.navigationView.setCheckedItem(R.id.nav_tab1)
//            }
//        }
//        pushFragment(mainFragment, getMainFragmentContainer(), isAddToBackStack = false)
//    }

    private fun switchTool(groupId: Int? = null, itemId: Int? = null) {
        navigationMenuData.also {
            if (groupId != null && itemId != null) {
                val group = it.getGroupList()[groupId]
                when (itemId) {
                    it.data[group]?.indexOf(Tool.TLV_PARSER) -> (mainFragment as CoreFragment).setCorePanel(Tool.TLV_PARSER)
                    it.data[group]?.indexOf(Tool.DES) -> (mainFragment as CoreFragment).setCorePanel(Tool.DES)
                    it.data[group]?.indexOf(Tool.HASH) -> (mainFragment as CoreFragment).setCorePanel(Tool.HASH)
                    it.data[group]?.indexOf(Tool.BITWISE) -> (mainFragment as CoreFragment).setCorePanel(Tool.BITWISE)
                    it.data[group]?.indexOf(Tool.MAC) -> (mainFragment as CoreFragment).setCorePanel(Tool.MAC)
                    it.data[group]?.indexOf(Tool.CONVERTER) -> (mainFragment as CoreFragment).setCorePanel(Tool.CONVERTER)
                    it.data[group]?.indexOf(Tool.RSA) -> (mainFragment as CoreFragment).setCorePanel(Tool.RSA)
                }
            } else {
                // Expand Generic tab by default
                binding.expandableMenu.expandGroup(0)
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