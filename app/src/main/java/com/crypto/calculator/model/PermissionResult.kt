package com.crypto.calculator.model

sealed class PermissionResult(val requestCode: Int) {
    /**
     * All permission granted, safe to do whatever you want
     */
    class PermissionGranted(requestCode: Int) : PermissionResult(requestCode)

    /**
     * Denied first time,
     * or request multiple permissions and got some denied
     */
    class PermissionDenied(requestCode: Int) : PermissionResult(requestCode)

    /**
     * When the user denied the requested permission,
     * Then the app should explain why it needs the permission
     */
    class NeedRationale(requestCode: Int) : PermissionResult(requestCode)

    /**
     * When user denied the permission(s) repeatedly, android see it is denied permanently.
     * Ideally you should ask user to manually go to settings and enable permission(s)
     */
    class PermissionDeniedPermanently(requestCode: Int) : PermissionResult(requestCode)

}

class PermissionRequest(
    var requestCode: Int? = null,
    var resultCallback: (PermissionResult.() -> Unit)? = null
)

abstract class PermissionRequestHandler {
    abstract fun onPermissionRequestedResult(resultCode: Int)
}