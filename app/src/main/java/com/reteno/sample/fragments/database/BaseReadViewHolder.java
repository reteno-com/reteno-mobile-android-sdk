package com.reteno.sample.fragments.database;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

abstract class BaseReadViewHolder<Model, ViewHolderBinding extends ViewBinding> extends RecyclerView.ViewHolder {
    final ViewHolderBinding bindingHolder;

    BaseReadViewHolder(ViewHolderBinding binding) {
        super(binding.getRoot());
        this.bindingHolder = binding;
    }


    protected abstract void bind(String timestamp, Model user);
}
