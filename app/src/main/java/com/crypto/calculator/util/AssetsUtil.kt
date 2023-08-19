package com.crypto.calculator.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson

object AssetsUtil {
    inline fun <reified T> readFile(context: Context, fileName: String): T {
        val dataStr = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val data = Gson().fromJson(dataStr, T::class.java)
        Log.d("AssetsUtil", "readFile - data:$data")
        return data
    }
}