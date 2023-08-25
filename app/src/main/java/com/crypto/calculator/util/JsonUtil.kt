package com.crypto.calculator.util

import android.util.Log
import com.crypto.calculator.extension.sorted
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object JsonUtil {
    fun flattenJson(json: String): List<String> {
        Log.d("flattenJson", "json: $json")
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)
        return flattenJsonObject("", jsonObject.sorted())
    }

    private fun flattenJsonObject(prefix: String, jsonObject: JsonObject): List<String> {
        val result = mutableListOf<String>()
        jsonObject.entrySet().forEach { (key, value) ->
            val newPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
            when (value) {
                is JsonObject -> result.addAll(flattenJsonObject(newPrefix, value))
                is JsonArray -> result.addAll(flattenJsonArray(newPrefix, value))
                else -> result.add("$newPrefix:${value.asString}")
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
                else -> result.add("$newPrefix:${jsonElement.asString}")
            }
        }
        return result
    }

    fun unflattenJson(flattenedList: List<String>): String {
        try {
            val root = mutableMapOf<String, Any>()
            flattenedList.forEach { item ->
                val split = item.split(":", limit = 2)
                val key = split[0]
                val value = split[1]
                val keyParts = key.split('.')
                var currentMap: MutableMap<String, Any> = root
                keyParts.dropLast(1).forEach { keyPart ->
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
                    list.add(listIndex, value)
                } else {
                    currentMap[keyParts.last()] = value
                }
            }
            return Gson().toJson(root.toSortedMap())
        } catch (e: Exception) {
            Log.e("unflattenJson", "Exception: $e")
            return ""
        }
    }
}