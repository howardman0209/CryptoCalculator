package com.crypto.calculator.ui.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.crypto.calculator.R
import com.crypto.calculator.databinding.DialogContentSingleInputBinding
import com.crypto.calculator.util.TAG
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class BaseFragment : Fragment() {
    protected lateinit var baseActivity: BaseActivity
    protected lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseActivity = activity as BaseActivity
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return setMainLayout(inflater, container)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //inflate base layout
        LayoutInflater.from(context).inflate(R.layout.fragment_base, view.parent as? ViewGroup)
    }

    fun onViewCreated(view: View, savedInstanceState: Bundle?, skip: Boolean) {
        super.onViewCreated(view, savedInstanceState)

        if (!skip) LayoutInflater.from(context)
            .inflate(R.layout.fragment_base, view.parent as ViewGroup)
    }

    @CallSuper
    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    open fun setMainLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(getLayoutResId(), container, false)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()

    }

    @LayoutRes
    abstract fun getLayoutResId(): Int

    abstract fun screenName(): String?

    open fun allowBackPress(): Boolean = true

    fun showLoadingIndicator(isShowLoading: Boolean) {
        baseActivity.showLoadingIndicator(isShowLoading)
    }

    fun singleInputDialog(context: Context, title: String?, fieldName: String, fieldValue: String?, onConfirmCallBack: (editText: String) -> Unit) {
        val dialogBinding: DialogContentSingleInputBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_content_single_input, null, false)
        dialogBinding.tiInputBox1.hint = fieldName
        dialogBinding.tiInputBox1.editText?.setText(fieldValue)

        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                onConfirmCallBack(dialogBinding.tiInputBox1.editText?.text.toString())
                Log.d(TAG, "confirm")
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                Log.d(TAG, "cancel")
            }
            .show()
    }

    // Prevent unintended double/ multiple click
    class PreventFastDoubleClick {
        companion object {
            private var lastClickTime: Long = 0
            fun isFastDoubleClick(): Boolean {
                val time = System.currentTimeMillis()
                val timeD = time - lastClickTime
                return if (timeD in 1..499) {
                    true
                } else {
                    lastClickTime = time
                    false
                }
            }

        }
    }

    fun View.setSingleClickListener(action: () -> Unit) {
        setOnClickListener {
            if (!PreventFastDoubleClick.isFastDoubleClick()) {
                action.invoke()
            } else {
                Log.d("Click Event", "== ! == Click Prevented")
            }
        }
    }
}