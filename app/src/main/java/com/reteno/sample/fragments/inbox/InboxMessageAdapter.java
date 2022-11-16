package com.reteno.sample.fragments.inbox;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.reteno.core.domain.model.appinbox.AppInboxMessage;
import com.reteno.sample.databinding.ItemAppinboxBinding;

public class InboxMessageAdapter extends ListAdapter<AppInboxMessage, InboxMessageAdapter.InboxViewHolder> {

    private final InboxItemClick itemClick;

    public InboxMessageAdapter(InboxItemClick itemClick) {
        super(DIFF_CALLBACK);
        this.itemClick = itemClick;
    }

    @NonNull
    @Override
    public InboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InboxViewHolder(
                ItemAppinboxBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                ),
                itemClick);
    }

    @Override
    public void onBindViewHolder(InboxViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class InboxViewHolder extends RecyclerView.ViewHolder {

        private final ItemAppinboxBinding binding;
        private final InboxItemClick itemClick;

        InboxViewHolder(ItemAppinboxBinding binding, InboxItemClick itemClick) {
            super(binding.getRoot());
            this.binding = binding;
            this.itemClick = itemClick;
        }

        public void bind(AppInboxMessage inbox) {
            binding.tvTitle.setText(inbox.getTitle());
            binding.tvData.setText(inbox.getCreatedDate());
            binding.tvContent.setText(inbox.getContent());
            binding.btnMarkAsOpened.setOnClickListener(v -> {
                itemClick.onOpenedClicked(inbox.getId());
            });
            binding.btnMarkAsOpened.setEnabled(inbox.isNewMessage());

            if (inbox.getLinkUrl() != null && !inbox.getLinkUrl().isEmpty()) {
                binding.cardView.setOnClickListener(v -> {
                    itemClick.onMessageClicked(inbox.getLinkUrl());
                });
            }

            if (inbox.getImageUrl() != null && !inbox.getImageUrl().isEmpty()) {
                Glide.with(binding.imageView)
                        .load(inbox.getImageUrl())
                        .into(binding.imageView);
            }

        }
    }

    public static final DiffUtil.ItemCallback<AppInboxMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppInboxMessage>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AppInboxMessage oldItem, @NonNull AppInboxMessage newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AppInboxMessage oldItem, @NonNull AppInboxMessage newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public interface InboxItemClick {

        void onOpenedClicked(String messageId);

        void onMessageClicked(String url);

    }
}
