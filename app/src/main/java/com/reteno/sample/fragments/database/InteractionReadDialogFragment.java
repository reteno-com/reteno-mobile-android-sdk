package com.reteno.sample.fragments.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.local.model.InteractionModelDb;
import com.reteno.sample.R;
import com.reteno.sample.databinding.ItemDbInteractionBinding;

import java.util.List;
import java.util.Locale;

import kotlin.Pair;


public class InteractionReadDialogFragment extends BaseReadDialogFragment<InteractionModelDb, ItemDbInteractionBinding, InteractionReadDialogFragment.InteractionViewHolder, InteractionReadDialogFragment.InteractionAdapter> {

    @Override
    protected void initAdapter() {
        adapter = new InteractionAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
    }

    @Override
    protected void initCount() {
        long count = databaseManager.getInteractionCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", count));
    }

    @Override
    protected void initItems() {
        List<Pair<String, InteractionModelDb>> newItems = databaseManager.getInteractions(null);
        adapter.setItems(newItems);
    }

    @Override
    protected void deleteItems(int count) {
        databaseManager.deleteInteractions(count, true);
    }

    //==============================================================================================
    static class InteractionAdapter extends BaseReadAdapter<InteractionModelDb, ItemDbInteractionBinding, InteractionViewHolder> {

        InteractionAdapter(ViewHolderListener listener) {
            super(listener);
        }

        @NonNull
        @Override
        public InteractionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbInteractionBinding binding = ItemDbInteractionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new InteractionViewHolder(binding);
        }


        @Override
        protected void initListeners(ItemDbInteractionBinding binding) {
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
    static class InteractionViewHolder extends BaseReadViewHolder<InteractionModelDb, ItemDbInteractionBinding> {

        InteractionViewHolder(ItemDbInteractionBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(String timestamp, InteractionModelDb interaction) {
            bindingHolder.tvValueTimestamp.setText(timestamp);

            bindingHolder.tvInteractionId.setText(interaction.getInteractionId());
            bindingHolder.tvStatus.setText(interaction.getStatus().toString());
            bindingHolder.tvTime.setText(interaction.getTime());
            bindingHolder.tvToken.setText(interaction.getToken());
        }
    }
}
