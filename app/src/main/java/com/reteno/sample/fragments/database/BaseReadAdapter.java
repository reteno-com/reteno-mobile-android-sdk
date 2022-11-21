package com.reteno.sample.fragments.database;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

import kotlin.Pair;

abstract class BaseReadAdapter<Model, ViewHolderBinding extends ViewBinding, ViewHolder extends BaseReadViewHolder<Model, ViewHolderBinding>> extends RecyclerView.Adapter<ViewHolder> {

    protected List<Model> items;
    protected final ViewHolderListener onExpandCollapseClickListener;

    BaseReadAdapter(ViewHolderListener listener) {
        this.onExpandCollapseClickListener = listener;
    }

    void setItems(List<Model> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public abstract ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Model item = items.get(position);
        initListeners(holder.bindingHolder);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    abstract void initListeners(ViewHolderBinding binding);
}
