package com.reteno.sample.fragments.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.local.model.event.EventDb;
import com.reteno.core.data.local.model.event.EventsDb;
import com.reteno.core.data.local.model.event.ParameterDb;
import com.reteno.sample.databinding.ItemDbEventBinding;
import com.reteno.sample.databinding.ViewEventReadBinding;
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding;

import java.util.List;
import java.util.Locale;


public class EventReadDialogFragment extends BaseReadDialogFragment<EventsDb, ItemDbEventBinding, EventReadDialogFragment.EventsViewHolder, EventReadDialogFragment.EventsAdapter> {

    @Override
    protected void initAdapter() {
        adapter = new EventsAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
    }

    @Override
    protected void initCount() {
        long eventsCount = databaseManager.getEventsCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", eventsCount));
    }

    @Override
    protected void deleteItems(int count) {
        databaseManager.deleteEvents(count, true);
    }

    @Override
    protected void initItems() {
        List<EventsDb> newEvents = databaseManager.getEvents(null);
        adapter.setItems(newEvents);
    }

    //==============================================================================================
    static class EventsAdapter extends BaseReadAdapter<EventsDb, ItemDbEventBinding, EventsViewHolder> {

        EventsAdapter(ViewHolderListener listener) {
            super(listener);
        }

        @NonNull
        @Override
        public EventsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbEventBinding binding = ItemDbEventBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new EventsViewHolder(binding);
        }

        @Override
        protected void initListeners(ItemDbEventBinding binding) {}
    }

    //==============================================================================================
    static class EventsViewHolder extends BaseReadViewHolder<EventsDb, ItemDbEventBinding> {

        EventsViewHolder(ItemDbEventBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(EventsDb events) {
            bindingHolder.tvDeviceId.setText(events.getDeviceId());
            bindingHolder.tvExternalUserId.setText(events.getExternalUserId());

            List<EventDb> eventList = events.getEventList();
            bindingHolder.llContent.removeAllViews();
            for (EventDb event : eventList) {
                View eventView = createNewEvent(bindingHolder.llContent, event);
                bindingHolder.llContent.addView(eventView);
            }
        }

        private View createNewEvent(ViewGroup parentViewGroup, EventDb event) {
            ViewEventReadBinding binding = ViewEventReadBinding.inflate(LayoutInflater
                    .from(bindingHolder.getRoot().getContext()), parentViewGroup, false);
            binding.tvEventTypeKey.setText(event.getEventTypeKey());
            binding.tvOccurred.setText(event.getOccurred());

            if (event.getParams() != null) {
                for (ParameterDb param : event.getParams()) {
                    View paramsView = createNewParams(binding.llEvent, param.getName(), param.getValue());
                    binding.llEvent.addView(paramsView);
                }
            }

            return binding.getRoot();
        }

        private View createNewParams(ViewGroup parentViewGroup, String key, String value) {
            ViewUserCustomFieldsVerticalBinding binding = ViewUserCustomFieldsVerticalBinding.inflate(LayoutInflater
                    .from(bindingHolder.getRoot().getContext()), parentViewGroup, false);
            binding.etKey.setText(key);
            binding.etValue.setText(value);
            return binding.getRoot();
        }
    }
}
