package com.crypto.calculator.ui.viewAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.TextView

class DropDownMenuAdapter(
    context: Context, private val layoutResource: Int,
    itemFullList: List<String>, private val unAssignItem: String? = null
) : ArrayAdapter<String>(context, layoutResource, itemFullList), Filterable {
    private var itemList: List<String> = itemFullList
    private var selectedItem: String? = null

    override fun getCount(): Int {
        return itemList.size
    }

    override fun getItem(p0: Int): String {
        selectedItem = itemList[p0]
        return if (unAssignItem.isNullOrEmpty()) {
            itemList[p0]
        } else {
            itemList[p0].ifEmpty {
                unAssignItem
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as TextView
        view.text = itemList[position].ifEmpty {
            unAssignItem
        }
        selectedItem = itemList[position]
        return view
    }
}