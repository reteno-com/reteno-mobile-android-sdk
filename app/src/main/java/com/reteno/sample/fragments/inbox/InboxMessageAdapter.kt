package com.reteno.sample.fragments.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reteno.core.domain.model.appinbox.AppInboxMessage
import com.reteno.sample.databinding.ItemAppinboxBinding

class InboxMessageAdapter(private val itemClick: InboxItemClick) :
    ListAdapter<AppInboxMessage, InboxMessageAdapter.InboxViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        return InboxViewHolder(
            ItemAppinboxBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            itemClick
        )
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InboxViewHolder(
        private val binding: ItemAppinboxBinding,
        private val itemClick: InboxItemClick
    ) : RecyclerView.ViewHolder(
        binding.getRoot()
    ) {
        fun bind(inbox: AppInboxMessage) {
            binding.tvTitle.text = inbox.title
            binding.tvData.text = inbox.createdDate
            binding.tvCategory.text = inbox.category
            binding.tvContent.text = inbox.content
            binding.tvStatus.text = inbox.status?.str
            binding.btnMarkAsOpened.setOnClickListener {
                itemClick.onOpenedClicked(inbox.id)
            }
            binding.btnMarkAsOpened.setEnabled(inbox.isNewMessage)
            inbox.linkUrl?.let { link ->
                binding.cardView.setOnClickListener {
                    itemClick.onMessageClicked(link)
                }
                Glide.with(binding.imageView)
                    .load(inbox.imageUrl)
                    .into(binding.imageView)
            }
            binding.imageView.isVisible = inbox.imageUrl != null && inbox.imageUrl!!.isNotEmpty()
            inbox.imageUrl?.let {
                Glide.with(binding.imageView)
                    .load(it)
                    .into(binding.imageView)
            }
            var customData = "empty"
            if (inbox.customData != null && inbox.customData!!.isNotEmpty()) {
                customData = inbox.customData.toString()
            }
            binding.tvCustomData.text = customData
        }
    }

    interface InboxItemClick {
        fun onOpenedClicked(messageId: String)
        fun onMessageClicked(url: String)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<AppInboxMessage> =
            object : DiffUtil.ItemCallback<AppInboxMessage>() {
                override fun areItemsTheSame(
                    oldItem: AppInboxMessage, newItem: AppInboxMessage
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: AppInboxMessage, newItem: AppInboxMessage
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
