package com.crypto.calculator.ui.base

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.akexorcist.localizationactivity.ui.LocalizationActivity
import com.crypto.calculator.R
import com.crypto.calculator.databinding.DialogContentEditConfigJsonBinding
import com.crypto.calculator.databinding.DialogContentSingleInputBinding
import com.crypto.calculator.extension.flatten
import com.crypto.calculator.extension.hideKeyboard
import com.crypto.calculator.extension.requireManageFilePermission
import com.crypto.calculator.extension.toSerializedMap
import com.crypto.calculator.model.PermissionRequest
import com.crypto.calculator.model.PermissionRequestHandler
import com.crypto.calculator.model.PermissionResult
import com.crypto.calculator.ui.viewAdapter.ConfigItemsAdapter
import com.crypto.calculator.uiComponent.ProgressDialog
import com.crypto.calculator.util.JsonUtil
import com.crypto.calculator.util.ShareFileUtil
import com.crypto.calculator.util.TAG
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import io.reactivex.disposables.Disposable


abstract class BaseActivity : LocalizationActivity() {
    private val permCallbackMap = mutableMapOf<Int, PermissionResult.() -> Unit>()
    lateinit var progressDialog: AlertDialog
    private var disposable: Disposable? = null
    var permissionRequestHandler: PermissionRequestHandler? = null
    val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("permissionRequestLauncher", "result: $result")
        permissionRequestHandler?.onPermissionRequestedResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Log.d(LIFECYCLE, "BaseActivity onCreate")

        setMainLayout()
        layoutInflater.inflate(R.layout.activity_base, findViewById(android.R.id.content))

        progressDialog = ProgressDialog(this).create()

    }

    open fun setMainLayout() {
        setContentView(getLayoutResId())
    }

    @LayoutRes
    abstract fun getLayoutResId(): Int

    @IdRes
    open fun getMainFragmentContainer(): Int? = null

    @CallSuper
    override fun onDestroy() {
        progressDialog.dismiss()

        if (disposable?.isDisposed == false) {
            disposable?.dispose()
        }
        permCallbackMap.clear()
        super.onDestroy()
    }

    inline fun requirePermissions(
        permissions: Array<out String>,
        requestBlock: PermissionRequest.() -> Unit
    ) {
        val permRequest = PermissionRequest().apply(requestBlock)
        requireNotNull(permRequest.requestCode) { "No requestCode specified." }
        requireNotNull(permRequest.resultCallback) { "No callback specified." }

        dispatchPermissionCheck(
            permissions,
            permRequest.requestCode!!,
            permRequest.resultCallback!!
        )
    }

    @PublishedApi
    internal fun dispatchPermissionCheck(
        permissions: Array<out String>,
        requestCode: Int,
        callback: PermissionResult.() -> Unit
    ) {
        permCallbackMap[requestCode] = callback
        val notGranted = permissions
            .filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }
            .toTypedArray()

        when {
            notGranted.isEmpty() -> onPermissionResult(
                PermissionResult.PermissionGranted(
                    requestCode
                )
            )

            notGranted.any { shouldShowRequestPermissionRationale(it) } -> onPermissionResult(
                PermissionResult.NeedRationale(requestCode)
            )

            else -> requestPermissions(notGranted, requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when {
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED } -> {
                // all perms granted after the result back
                onPermissionResult(PermissionResult.PermissionGranted(requestCode))
            }

            permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                // some denied & rationale might be needed
                onPermissionResult(PermissionResult.PermissionDenied(requestCode))
            }

            else -> {
                // repeatedly denied the permission -> os regards this as permanently denied
                // need to prompt user to app settings to change it
                onPermissionResult(PermissionResult.PermissionDeniedPermanently(requestCode))
            }
        }
    }

    private fun onPermissionResult(permissionResult: PermissionResult) {
        permCallbackMap[permissionResult.requestCode]?.let {
            permissionResult.it()
        }
        permCallbackMap.remove(permissionResult.requestCode)
    }

    open fun handleApiError(error: String) {
        showNonDisruptiveApiError(error)
    }

    private fun showNonDisruptiveApiError(message: String) {
        showLoadingIndicator(false)
        Snackbar.make(findViewById(R.id.baseActivityViewGroup), message, Snackbar.LENGTH_LONG).show()
    }

    fun startAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = Uri.fromParts("package", applicationContext.packageName, null)
                it.data = uri
                startActivity(it)
            }
    }

    fun appName() = getString(R.string.app_name)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                    v.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    fun showLoadingIndicator(isShowLoading: Boolean) {
        if (isShowLoading) {
            if (::progressDialog.isInitialized) {
                progressDialog.show()
            }
        } else {
            if (::progressDialog.isInitialized) {
                progressDialog.dismiss()
            }
        }
    }

    fun loadingIndicatorState(): Boolean {
        Log.d(TAG, "progressDialog.isInitialized: ${::progressDialog.isInitialized}")
        return if (::progressDialog.isInitialized) {
            progressDialog.isShowing
        } else {
            false
        }
    }

    enum class PushFragmentAction {
        Add,
        Replace
    }

    fun pushFragment(
        fragment: Fragment,
        @IdRes containerId: Int,
        action: PushFragmentAction = PushFragmentAction.Replace,
        isAddToBackStack: Boolean = true
    ) {
//        Log.d("DEBUG", "pushFragment")
        supportFragmentManager.beginTransaction().apply {
            if (action == PushFragmentAction.Replace) {
                replace(containerId, fragment)
            } else {
                add(containerId, fragment)
            }
            if (isAddToBackStack) {
                addToBackStack(fragment.javaClass.name)
            }
            commit()
        }
    }

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

    fun View.setSingleClickListener(a: () -> Unit) {
        setOnClickListener {
            if (!PreventFastDoubleClick.isFastDoubleClick()) {
                a.invoke()
            } else {
                Log.d("Click Event", "== ! == Click Prevented")
            }
        }
    }

    fun copyTextToClipboard(context: Context, copyText: String?, label: String? = null) {
        if (!copyText.isNullOrEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, copyText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, copyText, Toast.LENGTH_SHORT).show()
        }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun View.hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    fun singleInputDialog(context: Context, title: String?, fieldName: String, fieldValue: String? = null, onConfirmCallBack: (editText: String) -> Unit) {
        val dialogBinding: DialogContentSingleInputBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_content_single_input, null, false)
        dialogBinding.tiInputBox1.hint = fieldName
        dialogBinding.tiInputBox1.editText?.setText(fieldValue)

        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                Log.d(TAG, "confirm")
                dialogBinding.tiInputBox1.hideKeyboard()
                dialogBinding.tiInputBox1.post {
                    onConfirmCallBack(dialogBinding.tiInputBox1.editText?.text.toString())
                }
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                Log.d(TAG, "cancel")
            }
            .show()
    }

    fun arrayItemDialog(
        context: Context, items: Array<String?>, title: String?,
        positiveBtn: String? = null,
        positiveBtnCallback: (() -> Unit)? = null,
        negativeBtn: String? = null,
        negativeBtnCallback: (() -> Unit)? = null,
        neutralBtn: String? = null,
        neutralBtnCallback: (() -> Unit)? = null,
        onDismissCallback: (selectedOption: Int) -> Unit
    ) {
        var selectedOption: Int? = null
        MaterialAlertDialogBuilder(context)
            .setCancelable(true)
            .setTitle(title)
            .setItems(items) { _, index ->
                selectedOption = index
            }
            .apply {
                positiveBtnCallback?.let {
                    setPositiveButton(positiveBtn) { _, _ ->
                        it.invoke()
                    }
                }
                negativeBtnCallback?.let {
                    setNegativeButton(negativeBtn) { _, _ ->
                        it.invoke()
                    }
                }
                neutralBtnCallback?.let {
                    setNeutralButton(neutralBtn) { _, _ ->
                        it.invoke()
                    }
                }
            }
            .setOnDismissListener {
                selectedOption?.let { onDismissCallback.invoke(it) }
            }.show()
    }

    inline fun <reified T : Any> editConfigJson(
        context: Context, view: View,
        config: T,
        editable: Boolean = true,
        neutralBtn: String? = null,
        enableSaveLoadButton: Boolean = true,
        noinline onNeutralBtnClick: (() -> Unit)? = null,
        crossinline onConfirmClick: (editResult: T) -> Unit
    ) {
        val dialogBinding: DialogContentEditConfigJsonBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_content_edit_config_json, null, false)

        val map = config.toSerializedMap()
        val list = map.flatten().toMutableList()
        /// Do NOT sort the list otherwise cannot unflatten back to Json !!!

        val adapter = ConfigItemsAdapter { position, adapter ->
            if (editable) {
                val keyAndValue = list[position].split(':', limit = 2)
                singleInputDialog(context, getString(R.string.label_edit_item), keyAndValue.first(), keyAndValue.last()) { editedStr ->
                    list[position] = "${keyAndValue.first()}:$editedStr"
                    adapter.setData(list)
                }
            }
        }
        dialogBinding.rvConfigItems.adapter = adapter
        adapter.setData(list)

        if (!enableSaveLoadButton) {
            dialogBinding.appBarLayout.visibility = View.GONE
        }

        dialogBinding.saveBtn.setOnClickListener {
            requireManageFilePermission(it) {
                singleInputDialog(context, "Please input a file suffix", "Suffix") { suffix ->
                    if (suffix.isNotEmpty()) {
                        ShareFileUtil.saveConfigToJsonFile(context, config, suffix)
                    }
                }
            }
        }

        dialogBinding.loadBtn.setOnClickListener {
            requireManageFilePermission(it) {
                ShareFileUtil.loadConfigFromJsonFile(context, config) { jsonStr ->
                    Log.d(TAG, "jsonStr: $jsonStr")
                    list.clear()
                    list.addAll(JsonUtil.flattenJson(jsonStr))
                    adapter.setData(list)
                }
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.label_edit_item)
            .setCancelable(false)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                Log.d(TAG, "confirm")
                val editedJsonStr = JsonUtil.unflattenJson(list)
                try {
                    onConfirmClick.invoke(Gson().fromJson(editedJsonStr, T::class.java))
                } catch (e: Exception) {
                    Log.d(TAG, "Exception: $e")
                    Snackbar.make(view, "Invalid format", Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                Log.d(TAG, "cancel")
            }.apply {
                onNeutralBtnClick?.let {
                    setNeutralButton(neutralBtn) { _, _ ->
                        Log.d(TAG, "reset")
                        onNeutralBtnClick.invoke()
                    }
                }
            }
            .show()
    }
}