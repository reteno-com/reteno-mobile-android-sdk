package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.sample.databinding.ItemDbEventBinding
import com.reteno.sample.databinding.ViewEventReadBinding
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding
import com.reteno.sample.fragments.database.EventReadDialogFragment.EventsAdapter
import com.reteno.sample.fragments.database.EventReadDialogFragment.EventsViewHolder
import java.util.Locale

internal class EventReadDialogFragment :
    BaseReadDialogFragment<EventsDb, ItemDbEventBinding, EventsViewHolder, EventsAdapter>() {
    private var databaseManager: RetenoDatabaseManagerEvents? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerEventsProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun initAdapter() {
        adapter = EventsAdapter {
            TransitionManager.beginDelayedTransition(
                bindingMain!!.rvList, AutoTransition()
            )
        }
    }

    override fun initCount() {
        val eventsCount = databaseManager!!.getEventsCount()
        bindingMain!!.tvCount.text = String.format(Locale.US, "Count: %d", eventsCount)
    }

    override fun deleteItems(count: Int) {
        val events = databaseManager!!.getEvents(count)
        for (event in events) {
            databaseManager!!.deleteEvents(event)
        }
    }

    override fun initItems() {
        val newEvents = databaseManager!!.getEvents(null)
        adapter!!.setItems(newEvents)
    }

    //==============================================================================================
    internal class EventsAdapter(listener: ViewHolderListener) :
        BaseReadAdapter<EventsDb, ItemDbEventBinding, EventsViewHolder>(listener) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {
            val binding = ItemDbEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return EventsViewHolder(binding)
        }

        override fun initListeners(binding: ItemDbEventBinding) {}
    }

    //==============================================================================================
    internal class EventsViewHolder(binding: ItemDbEventBinding) :
        BaseReadViewHolder<EventsDb, ItemDbEventBinding>(binding) {
        override fun bind(model: EventsDb) {
            bindingHolder.tvDeviceId.setText(model.deviceId)
            bindingHolder.tvExternalUserId.setText(model.externalUserId)
            val eventList = model.eventList
            bindingHolder.llContent.removeAllViews()
            for (event in eventList) {
                val eventView = createNewEvent(bindingHolder.llContent, event)
                bindingHolder.llContent.addView(eventView)
            }
        }

        private fun createNewEvent(parentViewGroup: ViewGroup, event: EventDb): View {
            val binding = ViewEventReadBinding.inflate(
                LayoutInflater
                    .from(bindingHolder.getRoot().context), parentViewGroup, false
            )
            binding.tvEventTypeKey.text = event.eventTypeKey
            binding.tvOccurred.text = event.occurred
            if (event.params != null) {
                for ((name, value) in event.params!!) {
                    val paramsView = createNewParams(binding.llEvent, name, value)
                    binding.llEvent.addView(paramsView)
                }
            }
            return binding.getRoot()
        }

        private fun createNewParams(parentViewGroup: ViewGroup, key: String, value: String?): View {
            val binding = ViewUserCustomFieldsVerticalBinding.inflate(
                LayoutInflater
                    .from(bindingHolder.getRoot().context), parentViewGroup, false
            )
            binding.etKey.setText(key)
            binding.etValue.setText(value)
            return binding.getRoot()
        }
    }
}
