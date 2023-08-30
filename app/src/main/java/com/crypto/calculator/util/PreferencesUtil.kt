package com.crypto.calculator.util

import android.content.Context
import com.crypto.calculator.extension.toDataClass
import com.crypto.calculator.model.CapkList
import com.crypto.calculator.model.EmvConfig
import com.crypto.calculator.model.PaymentMethod
import com.crypto.calculator.model.Tool
import com.crypto.calculator.service.model.CardProfile
import com.google.gson.Gson
import java.util.Locale

object PreferencesUtil {

    fun clearPreferenceData(context: Context, path: String) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.remove(path)?.apply()
    }

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

    fun saveCardPreference(context: Context, card: PaymentMethod) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.putInt(prefCardPreference, card.id)?.apply()
    }

    fun getCardPreference(context: Context): PaymentMethod {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val id = localPref.getInt(prefCardPreference, PaymentMethod.VISA.id)
        return PaymentMethod.getById(id)
    }

    fun saveCardProfile(context: Context, cardProfile: CardProfile, cardScheme: PaymentMethod) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = Gson().toJson(cardProfile)
        localPref?.edit()?.putString("${cardScheme}-$prefCardProfile", jsonStr)?.apply()
    }

    fun getCardProfile(context: Context, cardScheme: PaymentMethod): CardProfile {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = localPref.getString("${cardScheme}-$prefCardProfile", null)
        val defaultPath = when (cardScheme) {
            PaymentMethod.VISA -> "${assetsPathCardVisa}_cvn10.json"
            PaymentMethod.MASTER -> "${assetsPathCardMaster}.json"
            PaymentMethod.UNIONPAY -> assetsPathCardUnionPay
            PaymentMethod.JCB -> assetsPathCardJcb
            PaymentMethod.DISCOVER -> assetsPathCardDiscover
            PaymentMethod.AMEX -> assetsPathCardAmex
            else -> ""
        }
        return jsonStr?.let {
            Gson().fromJson(it, CardProfile::class.java)
        } ?: AssetsUtil.readFile(context, defaultPath)
    }

    fun saveEmvConfig(context: Context, emvConfig: EmvConfig) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = Gson().toJson(emvConfig)
        localPref?.edit()?.putString(prefEmvConfig, jsonStr)?.apply()
    }

    fun getEmvConfig(context: Context): EmvConfig {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = localPref.getString(prefEmvConfig, null)
        return jsonStr?.toDataClass<EmvConfig>() ?: AssetsUtil.readFile(context, assetsPathTerminalEmvConfig)
    }

    fun saveCapkData(context: Context, capkData: CapkList) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = Gson().toJson(capkData)
        localPref?.edit()?.putString(prefCapkData, jsonStr)?.apply()
    }

    fun getCapkData(context: Context): CapkList {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = localPref.getString(prefCapkData, null)
        return jsonStr?.let {
            Gson().fromJson(jsonStr, CapkList::class.java)
        } ?: AssetsUtil.readFile(context, assetsPathTestCapk)
    }
}