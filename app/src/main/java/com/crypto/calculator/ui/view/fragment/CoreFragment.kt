package com.crypto.calculator.ui.view.fragment

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
import com.crypto.calculator.R
import com.crypto.calculator.databinding.FragmentCoreBinding
import com.crypto.calculator.model.Tools
import com.crypto.calculator.ui.base.MVVMFragment
import com.crypto.calculator.ui.viewModel.CoreViewModel

class CoreFragment(private var currentTool: Tools = Tools.TLV_PARSER) : MVVMFragment<CoreViewModel, FragmentCoreBinding>() {

    private var currentBottomPanel: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CoreFragment", "onCreate")
        switchBottomPanel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    fun setCorePanel(selectedTool: Tools) {
        Log.d("setCorePanel", "selectedTool: $selectedTool")
        currentTool = selectedTool
    }

    private fun switchBottomPanel(itemId: Int? = null, target: Int = R.id.bottomPanel) {
        currentBottomPanel = OutputFragment()
//            when (itemId) {
//                R.id.btnHome -> QuickLaunchFragment()
//                R.id.btnSupport -> SupportFragment()
//                R.id.btnMore -> MoreFragment()
//                R.id.btnPromotion -> PromotionsFragment()
//                else -> QuickLaunchFragment()
//            }
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
            R.id.action_settings -> viewModel.printLog("Hello World")
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