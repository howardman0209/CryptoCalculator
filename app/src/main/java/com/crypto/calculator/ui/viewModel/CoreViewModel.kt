package com.crypto.calculator.ui.viewModel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.crypto.calculator.model.Category
import com.crypto.calculator.model.Tool
import com.crypto.calculator.ui.base.BaseViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class CoreViewModel : BaseViewModel() {
    val gsonBeautifier: Gson = GsonBuilder().setPrettyPrinting().create()
    val currentTool: MutableLiveData<Tool> = MutableLiveData()
    val currentCategory: MutableLiveData<Category> = MutableLiveData()
    var infoMessage = ""
}