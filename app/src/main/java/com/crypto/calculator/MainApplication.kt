package com.crypto.calculator

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate
import com.crypto.calculator.model.Category
import com.crypto.calculator.model.NavigationMenuData
import com.crypto.calculator.model.Tool
import com.crypto.calculator.service.cardSimulator.CreditCardService
import com.crypto.calculator.ui.view.activity.MainActivity
import com.crypto.calculator.util.LIFECYCLE
import com.crypto.calculator.util.PreferencesUtil


class MainApplication : Application(), ActivityLifecycleCallbacks {

    companion object {
        var activitiesAlive = ArrayList<String>()

        fun getNavigationMenuData(): NavigationMenuData {
            return NavigationMenuData(
                data = hashMapOf(
                    Category.GENERIC to listOf(
                        Tool.TLV_PARSER,
                        Tool.DES,
                        Tool.HASH,
                        Tool.BITWISE,
                        Tool.MAC,
                        Tool.CONVERTER,
                        Tool.RSA,
                    ),
                    Category.EMV to listOf(
                        Tool.CARD_SIMULATOR,
                        Tool.EMV_KERNEL,
                        Tool.ARQC,
                        Tool.ODA,
                        Tool.PIN_BLOCK
                    )
                )
            )
        }
    }

    private var currentActiveActivity: Activity? = null
    private val localizationDelegate = LocalizationApplicationDelegate()

    override fun onCreate() {
        super.onCreate()
        Log.d(LIFECYCLE, "app onCreate")
        registerActivityLifecycleCallbacks(this)
        //fixme occasionally the view seems getting a mixed theme's attributes, hence disable the theme switching for now
        //val sharedPref: SharedPreferences = getSharedPreferences(PREF_THEME_NAME, Context.MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(LIFECYCLE, "${activity.javaClass.name} onCreated")
        if (!activitiesAlive.contains(activity.javaClass.name)) {
            activitiesAlive.add(activity.javaClass.name)
            Log.d(LIFECYCLE, activitiesAlive.toString())
        }
    }

    override fun onActivityStarted(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onStarted")
    }

    override fun onActivityResumed(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onResumed")
        currentActiveActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onPaused")
        currentActiveActivity = null
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(LIFECYCLE, "${activity.javaClass.name} onStopped")

        if (activity is MainActivity) {
            CreditCardService.enablePaymentService( false)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(LIFECYCLE, "${activity.javaClass.name} onDestroyed")
        activitiesAlive.remove(activity.javaClass.name)
    }

    override fun attachBaseContext(base: Context) {
        localizationDelegate.setDefaultLanguage(base, PreferencesUtil.getLocale(base))
        super.attachBaseContext(localizationDelegate.attachBaseContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localizationDelegate.onConfigurationChanged(this)
    }

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }
}