package com.crypto.calculator.extension

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices

@StringRes
fun Context.getStringResIdByKey(key: String): Int? {
    val resId = resources.getIdentifier(key, "string", packageName)
    return if (resId > 0) resId else null
}

fun Context.getDrawableOrNull(@DrawableRes id: Int?): Drawable? {
    return if (id == null || id == 0) null else AppCompatResources.getDrawable(this, id)
}

fun Context.getThemeColor(@AttrRes attribute: Int): ColorStateList {
    return TypedValue().let {
        theme.resolveAttribute(attribute, it, true)
        AppCompatResources.getColorStateList(this, it.resourceId)
    }
}

@SuppressLint("MissingPermission")
fun Context.getLocation(callback: (Location?) -> Unit) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        callback.invoke(null)
    } else {
        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mFusedLocationClient.lastLocation.addOnCompleteListener {

            //If the device do not have GMS (e.g. Apollo) it may throw exception during get location. Simply return null as location is not mandatory
            val location = try {
                it.result
            } catch (ex: Exception) {
                Log.d("Context", "Cannot get location result $ex")
                null
            }

            callback.invoke(location)
        }
    }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}