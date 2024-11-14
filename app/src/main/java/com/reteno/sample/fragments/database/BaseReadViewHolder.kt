package com.reteno.sample.fragments.database

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

internal abstract class BaseReadViewHolder<Model : Any, ViewHolderBinding : ViewBinding>(
    @JvmField val bindingHolder: ViewHolderBinding
) : RecyclerView.ViewHolder(bindingHolder.root) {
    abstract fun bind(model: Model)
}
