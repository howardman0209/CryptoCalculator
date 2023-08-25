package com.crypto.calculator.extension

import android.util.Log

fun Map<*, *>.flatten(prefix: String = ""): List<String> {
    val result = mutableListOf<String>()
    this.entries.forEach { (key, value) ->
        val newPrefix = if (prefix.isEmpty()) "$key" else "$prefix.$key"
        when (value) {
            is Map<*, *> -> result.addAll(value.flatten(newPrefix))
            is ArrayList<*> -> result.addAll(value.flatten(newPrefix))
            else -> result.add("$newPrefix:$value")
        }
    }
    return result
}

fun ArrayList<*>.flatten(prefix: String): List<String> {
    val result = mutableListOf<String>()
    this.forEachIndexed { index, value ->
        val newPrefix = "$prefix#$index"
        when (value) {
            is Map<*, *> -> result.addAll(value.flatten(newPrefix))
            is ArrayList<*> -> result.addAll(value.flatten(newPrefix))
            else -> result.add("$newPrefix:$value")
        }
    }
    return result
}

fun List<String>.unflatten(): Map<String, Any> {
    val root = mutableMapOf<String, Any>()
    try {
        this.forEach { item ->
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
        return root.toSortedMap()
    } catch (e: Exception) {
        Log.e("unflattenJson", "Exception: $e")
        return root
    }
}