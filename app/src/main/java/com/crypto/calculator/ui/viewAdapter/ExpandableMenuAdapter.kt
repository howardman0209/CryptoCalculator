package com.crypto.calculator.ui.viewAdapter

import android.content.Context
import android.graphics.Typeface.BOLD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.crypto.calculator.R
import com.crypto.calculator.model.Category
import com.crypto.calculator.model.Tool

class ExpandableMenuAdapter(
    private val context: Context,
    private val groupList: List<Category>,
    private val itemList: HashMap<Category, List<Tool>>,
    private val groupListLayoutResource: Int = R.layout.view_expandable_menu_group,
    private val itemListLayoutResource: Int = R.layout.view_expandable_menu_item,
) : BaseExpandableListAdapter() {
    override fun getChild(listPosition: Int, expandedListPosition: Int): Tool {
        return this.itemList[this.groupList[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val expandedListText = context.getString(getChild(listPosition, expandedListPosition).resourceId)
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(itemListLayoutResource, parent, false) as TextView
        view.text = expandedListText
        return view
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.itemList[this.groupList[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Category {
        return this.groupList[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.groupList.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val listTitle = context.getString(getGroup(listPosition).resourceId)
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(groupListLayoutResource, parent, false) as TextView
        view.text = listTitle
        view.setTypeface(null, BOLD)
        return view
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }
}