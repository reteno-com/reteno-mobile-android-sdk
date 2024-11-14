package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerAppInbox
import com.reteno.core.data.local.model.appinbox.AppInboxMessageDb
import com.reteno.sample.R
import com.reteno.sample.databinding.ItemDbInboxBinding
import com.reteno.sample.fragments.database.InboxReadDialogFragment.InboxAdapter
import java.util.Locale

internal class InboxReadDialogFragment :
    BaseReadDialogFragment<AppInboxMessageDb, ItemDbInboxBinding, InboxReadDialogFragment.InboxViewHolder, InboxAdapter>() {
    private var databaseManager: RetenoDatabaseManagerAppInbox? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerAppInboxProvider.get()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        databaseManager = null
    }

    override fun initAdapter() {
        adapter = InboxAdapter {
            TransitionManager.beginDelayedTransition(
                bindingMain!!.rvList, AutoTransition()
            )
        }
    }

    override fun initCount() {
        val count = databaseManager!!.getAppInboxMessagesCount()
        bindingMain!!.tvCount.text = String.format(Locale.US, "Count: %d", count)
    }

    override fun initItems() {
        val newItems = databaseManager!!.getAppInboxMessages(null)
        adapter!!.setItems(newItems)
    }

    override fun deleteItems(count: Int) {
        val messages = databaseManager!!.getAppInboxMessages(count)
        databaseManager!!.deleteAppInboxMessages(messages)
    }

    //==============================================================================================
    internal class InboxAdapter(listener: ViewHolderListener) :
        BaseReadAdapter<AppInboxMessageDb, ItemDbInboxBinding, InboxViewHolder>(listener) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
            val binding =
                ItemDbInboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return InboxViewHolder(binding)
        }

        override fun initListeners(binding: ItemDbInboxBinding) {
            binding.ivExpand.setOnClickListener { v: View? ->
                if (binding.llContent.visibility == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.GONE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more)
                } else {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.VISIBLE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less)
                }
            }
        }
    }

    //==============================================================================================
    class InboxViewHolder(binding: ItemDbInboxBinding) :
        BaseReadViewHolder<AppInboxMessageDb, ItemDbInboxBinding>(binding) {
            override fun bind(model: AppInboxMessageDb) {
            bindingHolder.tvInboxId.setText(model.id)
            bindingHolder.tvStatus.setText(model.status.toString())
            bindingHolder.tvTime.setText(model.occurredDate)
            bindingHolder.tvDeviceId.setText(model.deviceId)
        }
    }
}
