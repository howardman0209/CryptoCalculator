package com.crypto.calculator.ui.viewAdapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.crypto.calculator.databinding.ViewHolderMapDataBinding

class ConfigItemsAdapter(private val onClickCallback: (idx: Int, adapter: ConfigItemsAdapter) -> Unit) : RecyclerView.Adapter<ItemViewHolder>() {
    private var dataList: ArrayList<String> = arrayListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val keyAndValue = dataList[position].split(':', limit = 2)
        holder.bind(keyAndValue.first(), keyAndValue.last())

        holder.itemView.setOnClickListener {
            onClickCallback.invoke(position, this)
        }
    }

    fun setData(dataList: List<String>) {
        val diffResult = DiffUtil.calculateDiff(
            ItemDiffCallback(
                this.dataList.toList(),
                dataList.toList()
            )
        )
        this.dataList.clear()
        this.dataList.addAll(dataList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class ItemDiffCallback(
    private val oldList: List<String>,
    private val newList: List<String>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItemPosition == newItemPosition
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}

class ItemViewHolder(private val binding: ViewHolderMapDataBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(key: String, value: String) {
        binding.tvKey.text = key
        binding.tvValue.text = value
    }

    companion object {
        fun from(parent: ViewGroup): ItemViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ViewHolderMapDataBinding.inflate(layoutInflater, parent, false)
            return ItemViewHolder(binding)
        }
    }
}