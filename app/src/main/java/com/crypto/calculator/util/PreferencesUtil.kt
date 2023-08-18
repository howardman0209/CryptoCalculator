package com.crypto.calculator.util

import android.content.Context
import com.crypto.calculator.model.Tool
import java.util.Locale

object PreferencesUtil {

    fun saveLocale(context: Context?, locale: Locale) {
        val localPref = context?.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.apply {
            putString(localeLanguagePrefKey, locale.language)
            putString(localeCountryPrefKey, locale.country)
            apply()
        }
    }

    fun getLocale(context: Context?): Locale {
        if (context == null) return Locale.getDefault()
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val localeLanguage = localPref.getString(localeLanguagePrefKey, "").orEmpty()
        val localeCountry = localPref.getString(localeCountryPrefKey, "").orEmpty()
        return if (localeLanguage.isEmpty() && localeCountry.isEmpty()) {
            val deviceLocale = context.resources.configuration.locales.get(0)
            Locale(deviceLocale.language, deviceLocale.country)
        } else {
            Locale(localeLanguage, localeCountry)
        }
    }


    fun getLocaleInfo(context: Context?, prefKey: String): String {
        if (context == null) return ""
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        return localPref.getString(prefKey, "").orEmpty()
    }

    fun saveLastUsedTool(context: Context, tool: Tool) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.putInt(prefLastUsedTool, tool.id)?.apply()
    }

    fun getLastUsedTool(context: Context): Tool {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val id = localPref.getInt(prefLastUsedTool, Tool.TLV_PARSER.id)
        return Tool.getById(id)
    }
}