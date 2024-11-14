package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.sample.R
import com.reteno.sample.databinding.ItemDbInteractionBinding
import com.reteno.sample.fragments.database.InteractionReadDialogFragment.InteractionAdapter
import com.reteno.sample.fragments.database.InteractionReadDialogFragment.InteractionViewHolder
import java.util.Locale

internal class InteractionReadDialogFragment :
    BaseReadDialogFragment<InteractionDb, ItemDbInteractionBinding, InteractionViewHolder, InteractionAdapter>() {
    private var databaseManager: RetenoDatabaseManagerInteraction? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerInteractionProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun initAdapter() {
        adapter = InteractionAdapter {
            TransitionManager.beginDelayedTransition(
                bindingMain!!.rvList, AutoTransition()
            )
        }
    }

    override fun initCount() {
        val count = databaseManager!!.getInteractionCount()
        bindingMain!!.tvCount.text = String.format(Locale.US, "Count: %d", count)
    }

    override fun initItems() {
        val newItems = databaseManager!!.getInteractions(null)
        adapter!!.setItems(newItems)
    }

    override fun deleteItems(count: Int) {
        val interactions = databaseManager!!.getInteractions(count)
        for (interaction in interactions) {
            databaseManager!!.deleteInteraction(interaction)
        }
    }

    //==============================================================================================
    internal class InteractionAdapter(listener: ViewHolderListener) :
        BaseReadAdapter<InteractionDb, ItemDbInteractionBinding, InteractionViewHolder>(listener) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InteractionViewHolder {
            val binding =
                ItemDbInteractionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return InteractionViewHolder(binding)
        }

        override fun initListeners(binding: ItemDbInteractionBinding) {
            binding.ivExpand.setOnClickListener {
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
    class InteractionViewHolder(binding: ItemDbInteractionBinding) :
        BaseReadViewHolder<InteractionDb, ItemDbInteractionBinding>(binding) {
        override fun bind(model: InteractionDb) {
            bindingHolder.tvInteractionId.setText(model.interactionId)
            bindingHolder.tvStatus.setText(model.status.toString())
            bindingHolder.tvTime.setText(model.time)
            bindingHolder.tvToken.setText(model.token)
        }
    }
}
