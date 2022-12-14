package com.reteno.sample.testscreens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.reteno.sample.databinding.ItemButtonGoToFragmentBinding;

import java.util.List;

public class ScreenAdapter extends ListAdapter<ScreenItem, ScreenAdapter.ScreenViewHolder> {

    private final ScreenItemClick itemClick;

    public ScreenAdapter(List<ScreenItem> screens, ScreenItemClick itemClick) {
        super(DIFF_CALLBACK);
        this.itemClick = itemClick;
        submitList(screens);
    }

    @NonNull
    @Override
    public ScreenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScreenViewHolder(
                ItemButtonGoToFragmentBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                ),
                itemClick);
    }

    @Override
    public void onBindViewHolder(ScreenViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ScreenViewHolder extends RecyclerView.ViewHolder {

        private final ItemButtonGoToFragmentBinding binding;
        private final ScreenItemClick itemClick;

        ScreenViewHolder(ItemButtonGoToFragmentBinding binding, ScreenItemClick itemClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.itemClick = itemClick;
        }

        public void bind(ScreenItem screen) {
            binding.buttonItem.setText(screen.getName());

            binding.buttonItem.setOnClickListener(v -> {
                if (screen.getNavigationId() != -1) {
                    if (screen.getBundle() != null) {
                        itemClick.navigateById(screen.getNavigationId(), screen.getBundle());
                    } else {
                        itemClick.navigateById(screen.getNavigationId());
                    }
                } else if (screen.getDirection() != null) {
                    itemClick.navigateByDirections(screen.getDirection());
                }
            });

        }
    }

    public static final DiffUtil.ItemCallback<ScreenItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ScreenItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ScreenItem oldItem, @NonNull ScreenItem newItem) {
                    return oldItem.getName().equals(newItem.getName());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ScreenItem oldItem, @NonNull ScreenItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public interface ScreenItemClick {

        void navigateById(int fragmentId);

        void navigateById(int fragmentId, Bundle bundle);

        void navigateByDirections(NavDirections navDirections);
    }
}
