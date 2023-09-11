package com.crypto.calculator.ui.view.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.crypto.calculator.R
import com.crypto.calculator.databinding.ActivitySettingBinding
import com.crypto.calculator.databinding.DialogLogFontSettingBinding
import com.crypto.calculator.model.CapkList
import com.crypto.calculator.ui.base.MVVMActivity
import com.crypto.calculator.ui.viewModel.SettingViewModel
import com.crypto.calculator.util.AssetsUtil
import com.crypto.calculator.util.PreferencesUtil
import com.crypto.calculator.util.assetsPathLiveCapk
import com.crypto.calculator.util.assetsPathTestCapk
import com.crypto.calculator.util.prefImkList
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingActivity : MVVMActivity<SettingViewModel, ActivitySettingBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SettingActivity", "onCreate")

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.settingFont.setOnClickListener {
            Log.d("SettingActivity", "settingFont")
            val dialogBinding: DialogLogFontSettingBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_log_font_setting, null, false)
            dialogBinding.sliderFontSize.apply {
                //init view
                value = PreferencesUtil.getLogFontSize(applicationContext)
                setLabelFormatter { value ->
                    getString(R.string.var_font_size).format(value.toInt().toString())
                }
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.label_setting_log_font_size)
                .setCancelable(false)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.button_confirm) { _, _ ->
                    Log.d("settingFont", "confirm")
                    Log.d("settingFont", "value: ${dialogBinding.sliderFontSize.value}")
                    PreferencesUtil.saveLogFontSize(applicationContext, dialogBinding.sliderFontSize.value)
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    Log.d("settingFont", "cancel")
                }
                .show()
        }

        binding.settingCAPK.setOnClickListener {
            editConfigJson(this, it, PreferencesUtil.getCapkData(applicationContext), true,
                neutralBtn = getString(R.string.button_reset),
                onNeutralBtnClick = {
                    arrayItemDialog(this, items = arrayOf("Live", "Test"), title = "Choose environment", onDismissCallback = { selected ->
                        val capkList = when (selected) {
                            0 -> AssetsUtil.readFile<CapkList>(applicationContext, assetsPathLiveCapk)
                            else -> AssetsUtil.readFile<CapkList>(applicationContext, assetsPathTestCapk)
                        }
                        editConfigJson(this, it, capkList, false, onConfirmClick = { edited -> PreferencesUtil.saveCapkData(applicationContext, edited) })
                    })
                }
            ) { editedCapk ->
                PreferencesUtil.saveCapkData(applicationContext, editedCapk)
            }
        }

        binding.settingIMK.setOnClickListener {
            val imkList = PreferencesUtil.getIMKMap(applicationContext)
            editConfigJson(this, it, imkList, true,
                neutralBtn = getString(R.string.button_reset),
                onNeutralBtnClick = {
                    PreferencesUtil.clearPreferenceData(applicationContext, prefImkList)
                }
            ) { editedImk ->
                PreferencesUtil.saveIMKMap(applicationContext, editedImk)
            }
        }
    }

    override fun getViewModelInstance(): SettingViewModel = ViewModelProvider(this)[SettingViewModel::class.java]
    override fun setBindingData() {
        binding.viewModel = viewModel
        binding.view = this
    }

    override fun getLayoutResId(): Int = R.layout.activity_setting
}