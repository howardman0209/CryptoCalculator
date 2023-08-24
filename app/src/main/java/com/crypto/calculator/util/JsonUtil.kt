package com.crypto.calculator.util

import android.util.Log
import com.crypto.calculator.extension.sorted
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object JsonUtil {
    /// Do NOT sort the list otherwise cannot unflatten back to Json !!!
    fun flattenJson(json: String): List<String> {
        Log.d("flattenJson", "json: $json")
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)
        /// Do NOT sort the list otherwise cannot unflatten back to Json !!!
        return flattenJsonObject("", jsonObject.sorted())
    }

    private fun flattenJsonObject(prefix: String, jsonObject: JsonObject): List<String> {
        val result = mutableListOf<String>()
        for ((key, value) in jsonObject.entrySet()) {
            val newPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
            when (value) {
                is JsonObject -> result.addAll(flattenJsonObject(newPrefix, value))
                is JsonArray -> result.addAll(flattenJsonArray(newPrefix, value))
                else -> result.add("$newPrefix:$value")
            }
        }
        return result
    }

    private fun flattenJsonArray(prefix: String, jsonArray: JsonArray): List<String> {
        val result = mutableListOf<String>()
        jsonArray.forEachIndexed { index, jsonElement ->
            val newPrefix = "$prefix#$index"
            when (jsonElement) {
                is JsonObject -> result.addAll(flattenJsonObject(newPrefix, jsonElement))
                is JsonArray -> result.addAll(flattenJsonArray(newPrefix, jsonElement))
                else -> result.add("$newPrefix:$jsonElement")
            }
        }
        return result
    }

    fun unflattenJson(flattenedList: List<String>): String {
        try {
            val root = mutableMapOf<String, Any>()
            for (item in flattenedList) {
                val split = item.split(":", limit = 2)
                val key = split[0]
                val value = split[1]
                val keyParts = key.split('.')
//            Log.d("unflattenJson", "keyParts: $keyParts")
                var currentMap: MutableMap<String, Any> = root
                for (i in 0 until keyParts.size - 1) {
                    val keyPart = keyParts[i]
                    currentMap = if (keyPart.contains("#")) {
                        val arrayKey = keyPart.split('#')
                        val listIndex = arrayKey.last().toInt()
                        val list = currentMap.getOrPut(arrayKey.first()) { mutableListOf<Any>() } as MutableList<Any>
                        if (list.size <= listIndex) {
                            list.add(mutableMapOf<String, Any>())
                        }
                        list[listIndex] as MutableMap<String, Any>
                    } else {
                        currentMap.getOrPut(keyPart) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
                    }
                }
                if (keyParts.last().contains("#")) {
                    val arrayKey = keyParts.last().split('#')
                    val listIndex = arrayKey.last().toInt()
                    val list = currentMap.getOrPut(arrayKey.first()) { mutableListOf<Any>() } as MutableList<Any>
//                Log.d("unflattenJson", "value: $value")

                    list.add(listIndex, Gson().fromJson(value, JsonElement::class.java))
                } else {
                    currentMap[keyParts.last()] = Gson().fromJson(value, JsonElement::class.java)
                }
            }
            return Gson().toJson(root.toSortedMap())
        } catch (e: Exception) {
            Log.e("unflattenJson", "Exception: $e")
            return ""
        }
    }
}