package com.crypto.calculator.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class MVVMFragment<VM : BaseViewModel, BINDING : ViewDataBinding> : BaseFragment() {
    protected lateinit var viewModel: VM
    protected lateinit var binding: BINDING

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = getViewModelInstance()

        viewModel.isShowLoading.observe(this) {
            showLoadingIndicator(it == true)
        }

        viewModel.apiError.observe(this) {
            if (it != null) {
                handleApiError(it)
            }
        }
    }

    protected open fun handleApiError(error: String) {
        baseActivity.handleApiError(error)
        viewModel.apiError.value = null
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()

        /**
         * Dispose all disposables
         * e.g. disposable of api call
         */
        viewModel.getDisposableList().forEach {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    override fun setMainLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = DataBindingUtil.inflate(inflater, getLayoutResId(), container, false)

        setBindingData()

        return binding.root
    }

    abstract fun getViewModelInstance(): VM

    abstract fun setBindingData()
}