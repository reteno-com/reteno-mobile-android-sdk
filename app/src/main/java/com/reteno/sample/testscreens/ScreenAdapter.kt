package com.reteno.sample.testscreens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reteno.sample.databinding.ItemButtonGoToFragmentBinding
import com.reteno.sample.testscreens.ScreenAdapter.ScreenViewHolder

class ScreenAdapter(screens: List<ScreenItem>?, private val itemClick: ScreenItemClick) :
    ListAdapter<ScreenItem, ScreenViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenViewHolder {
        return ScreenViewHolder(
            ItemButtonGoToFragmentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            itemClick
        )
    }

    override fun onBindViewHolder(holder: ScreenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScreenViewHolder(
        private val binding: ItemButtonGoToFragmentBinding,
        private val itemClick: ScreenItemClick
    ) : RecyclerView.ViewHolder(
        binding.getRoot()
    ) {
        fun bind(screen: ScreenItem) {
            binding.buttonItem.text = screen.name
            binding.buttonItem.setOnClickListener {
                if (screen.navigationId != -1) {
                    if (screen.bundle != null) {
                        itemClick.navigateById(screen.navigationId, screen.bundle)
                    } else {
                        itemClick.navigateById(screen.navigationId)
                    }
                } else if (screen.direction != null) {
                    itemClick.navigateByDirections(screen.direction)
                }
            }
        }
    }

    init {
        submitList(screens)
    }

    interface ScreenItemClick {
        fun navigateById(fragmentId: Int)
        fun navigateById(fragmentId: Int, bundle: Bundle)
        fun navigateByDirections(navDirections: NavDirections)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<ScreenItem> =
            object : DiffUtil.ItemCallback<ScreenItem>() {
                override fun areItemsTheSame(
                    oldItem: ScreenItem, newItem: ScreenItem
                ): Boolean {
                    return oldItem.name == newItem.name
                }

                override fun areContentsTheSame(
                    oldItem: ScreenItem, newItem: ScreenItem
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
