package com.reteno.sample.fragments.database;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox;
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb;
import com.reteno.sample.R;
import com.reteno.sample.databinding.ItemDbInboxBinding;

import java.util.List;
import java.util.Locale;


public class InboxReadDialogFragment extends BaseReadDialogFragment<AppInboxMessageDb, ItemDbInboxBinding, InboxReadDialogFragment.InboxViewHolder, InboxReadDialogFragment.InboxAdapter> {

    private RetenoDatabaseManagerAppInbox databaseManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        databaseManager = serviceLocator.getRetenoDatabaseManagerAppInboxProvider().get();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        databaseManager = null;
    }

    @Override
    protected void initAdapter() {
        adapter = new InboxAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
    }

    @Override
    protected void initCount() {
        long count = databaseManager.getAppInboxMessagesCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", count));
    }

    @Override
    protected void initItems() {
        List<AppInboxMessageDb> newItems = databaseManager.getAppInboxMessages(null);
        adapter.setItems(newItems);
    }

    @Override
    protected void deleteItems(int count) {
        databaseManager.deleteAppInboxMessages(count, true);
    }

    //==============================================================================================
    static class InboxAdapter extends BaseReadAdapter<AppInboxMessageDb, ItemDbInboxBinding, InboxViewHolder> {

        InboxAdapter(ViewHolderListener listener) {
            super(listener);
        }

        @NonNull
        @Override
        public InboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbInboxBinding binding = ItemDbInboxBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new InboxViewHolder(binding);
        }


        @Override
        protected void initListeners(ItemDbInboxBinding binding) {
            binding.ivExpand.setOnClickListener(v -> {
                if (binding.llContent.getVisibility() == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.GONE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more);
                } else {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.VISIBLE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less);
                }
            });
        }
    }

    //==============================================================================================
    static class InboxViewHolder extends BaseReadViewHolder<AppInboxMessageDb, ItemDbInboxBinding> {

        InboxViewHolder(ItemDbInboxBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(AppInboxMessageDb interaction) {
            bindingHolder.tvInboxId.setText(interaction.getId());
            bindingHolder.tvStatus.setText(interaction.getStatus().toString());
            bindingHolder.tvTime.setText(interaction.getOccurredDate());
            bindingHolder.tvDeviceId.setText(interaction.getDeviceId());
        }
    }
}
