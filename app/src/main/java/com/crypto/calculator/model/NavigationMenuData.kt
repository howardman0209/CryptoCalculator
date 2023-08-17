package com.crypto.calculator.model

data class NavigationMenuData(
    val data: HashMap<String, List<Tool>>
)

fun NavigationMenuData.getGroupList(): List<String> {
    return this.data.keys.toList()
}