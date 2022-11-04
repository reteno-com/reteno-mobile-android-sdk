package com.reteno.sample.fragments.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.model.Event;
import com.reteno.core.model.Events;
import com.reteno.core.model.Parameter;
import com.reteno.sample.databinding.ItemDbEventBinding;
import com.reteno.sample.databinding.ViewEventReadBinding;
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import kotlin.Pair;


public class EventReadDialogFragment extends BaseReadDialogFragment<Events, ItemDbEventBinding, EventReadDialogFragment.EventsViewHolder, EventReadDialogFragment.EventsAdapter> {

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
        List<Events> newEvents = databaseManager.getEvents(null);
        adapter.setItems(newEvents);
    }

    //==============================================================================================
    static class EventsAdapter extends BaseReadAdapter<Events, ItemDbEventBinding, EventsViewHolder> {

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
    static class EventsViewHolder extends BaseReadViewHolder<Events, ItemDbEventBinding> {

        EventsViewHolder(ItemDbEventBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(Events events) {
            bindingHolder.tvDeviceId.setText(events.getDeviceId());
            bindingHolder.tvExternalUserId.setText(events.getExternalUserId());

            List<Event> eventList = events.getEventList();
            bindingHolder.llContent.removeAllViews();
            for (Event event : eventList) {
                View eventView = createNewEvent(bindingHolder.llContent, event);
                bindingHolder.llContent.addView(eventView);
            }
        }

        private View createNewEvent(ViewGroup parentViewGroup, Event event) {
            ViewEventReadBinding binding = ViewEventReadBinding.inflate(LayoutInflater
                    .from(bindingHolder.getRoot().getContext()), parentViewGroup, false);
            binding.tvEventTypeKey.setText(event.getEventTypeKey());
            binding.tvOccurred.setText(event.getOccurred().toString());

            if (event.getParams() != null) {
                for (Parameter param : event.getParams()) {
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
