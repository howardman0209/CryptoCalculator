package com.crypto.calculator.ui.view.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.MainApplication
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentCoreBinding
import com.crypto.calculator.model.Category
import com.crypto.calculator.model.NavigationMenuData
import com.crypto.calculator.model.Tool
import com.crypto.calculator.model.getGroupList
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.view.activity.MainActivity
import com.crypto.calculator.ui.view.activity.SettingActivity
import com.crypto.calculator.ui.viewModel.CoreViewModel
import com.crypto.calculator.util.LogPanelUtil
import com.crypto.calculator.util.LongLogUtil
import com.crypto.calculator.util.PreferencesUtil

class CoreFragment : MVVMFragment<CoreViewModel, FragmentCoreBinding>() {

    private lateinit var navigationMenuData: NavigationMenuData
    private var currentCorePanel: Fragment? = null
    private var currentBottomPanel: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CoreFragment", "onCreate")

        navigationMenuData = MainApplication.getNavigationMenuData()
        viewModel.currentTool.postValue(PreferencesUtil.getLastUsedTool(requireContext().applicationContext))
        switchBottomPanel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CoreFragment", "onViewCreated")

        viewModel.currentTool.observe(viewLifecycleOwner) {
            navigationMenuData.getGroupList().find { group ->
                navigationMenuData.data[group]?.contains(it) == true
            }?.also {
                if (viewModel.currentCategory.value != it) {
                    viewModel.currentCategory.postValue(it)
                }
            }
        }

        viewModel.currentCategory.observe(viewLifecycleOwner) {
            switchCorePanel(it)
        }
    }

    fun setCurrentTool(selectedTool: Tool) {
        Log.d("setCurrentTool", "selectedTool: $selectedTool")
        viewModel.currentTool.postValue(selectedTool)
    }

    private fun switchCorePanel(parentGroup: Category, target: Int = R.id.corePanel) {
        val selectedFragment = when (parentGroup) {
            Category.GENERIC -> InputFragment()
            Category.EMV -> EmvFragment()
            else -> null
        }

        selectedFragment?.let {
            currentCorePanel = selectedFragment
            pushFragment(selectedFragment, target, isAddToBackStack = false)
        }
    }

    private fun switchBottomPanel(target: Int = R.id.bottomPanel) {
        currentBottomPanel = OutputFragment()
        pushFragment(currentBottomPanel as Fragment, target, isAddToBackStack = false)
    }

    private fun logcatSwitch() {
        when {
            binding.bottomPanel.visibility == View.VISIBLE && binding.corePanel.visibility == View.VISIBLE -> {
                binding.corePanel.visibility = View.GONE
            }

            binding.corePanel.visibility == View.GONE -> {
                binding.corePanel.visibility = View.VISIBLE
                binding.bottomPanel.visibility = View.GONE
            }

            binding.corePanel.visibility == View.VISIBLE && binding.bottomPanel.visibility == View.GONE -> {
                binding.corePanel.visibility = View.VISIBLE
                binding.bottomPanel.visibility = View.VISIBLE
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("onOptionsItemSelected", "item: $item")
        when (item.itemId) {
            R.id.action_settings -> {
//                singleInputDialog(requireContext(), "Enter your message", "Message") {
//                    LogPanelUtil.printLog(it)
//                }
                startActivity(Intent(requireContext().applicationContext, SettingActivity::class.java))
            }

            R.id.action_test -> {
                val test = ""
                LongLogUtil.debug("@@", "test: $test")
                LogPanelUtil.printLog("test: $test")
            }

            R.id.tool_logcat -> logcatSwitch()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.tools, menu)
        super.onCreateOptionsMenu(menu, inflater)
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