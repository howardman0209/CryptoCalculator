package com.crypto.calculator.extension

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonObject.findByKey(key: String): List<JsonElement> {
    val result: MutableList<JsonElement> = mutableListOf()
    this.entrySet().forEach {
        if (it.key == key) {
            Log.d("findByKey", "found - key:${it.key} value:${it.value}")
            result.add(it.value)
        }
        when (it.value) {
            is JsonObject -> {
                val value = it.value.asJsonObject
                result.addAll(value.findByKey(key))
            }

            is JsonArray -> {
                val value = it.value.asJsonArray
                result.addAll(value.findByKey(key))
            }

            else -> {}
        }
    }
    return result
}

fun JsonArray.findByKey(key: String): List<JsonElement> {
    val result: MutableList<JsonElement> = mutableListOf()
    this.forEach {
        when (it) {
            is JsonObject -> {
                val value = it.asJsonObject
                result.addAll(value.findByKey(key))
            }

            is JsonArray -> {
                val value = it.asJsonArray
                result.addAll(value.findByKey(key))
            }

            else -> {}
        }
    }
    return result
}

fun JsonObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    this.entrySet().forEach {
        var value: Any = it.value
        when (it.value) {
            is JsonArray -> value = it.value.asJsonArray.toList()
            is JsonObject -> value = it.value.asJsonObject.toMap()
        }
        map[it.key] = value
    }
    return map.toSortedMap()
}

fun JsonArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    this.forEach {
        var value: Any = it
        when (value) {
            is JsonArray -> value = it.asJsonArray.toList()
            is JsonObject -> value = it.asJsonObject.toMap()
        }
        list.add(value)
    }
    return list
}

fun JsonObject.sorted(): JsonObject {
    return Gson().toJson(this.toMap().toSortedMap()).toDataClass()
}