package com.reteno.sample.fragments.database

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

internal abstract class BaseReadAdapter<Model : Any, ViewHolderBinding : ViewBinding, ViewHolder : BaseReadViewHolder<Model, ViewHolderBinding>>(
    @JvmField protected val onExpandCollapseClickListener: ViewHolderListener
) : RecyclerView.Adapter<ViewHolder>() {

    private var itemsList: List<Model>? = null

    fun setItems(newItems: List<Model>?) {
        itemsList = newItems
        notifyDataSetChanged()
    }

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemsList!![position]
        initListeners(holder.bindingHolder)
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return itemsList!!.size
    }

    abstract fun initListeners(binding: ViewHolderBinding)
}
