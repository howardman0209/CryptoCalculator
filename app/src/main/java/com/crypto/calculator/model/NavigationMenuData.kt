package com.crypto.calculator.model

data class NavigationMenuData(
    val data: HashMap<Category, List<Tool>>
)

fun NavigationMenuData.getGroupList(): List<Category> {
    return this.data.keys.toList()
}