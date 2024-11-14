package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentEcomEventsBinding

class FragmentEcomEvents : BaseFragment() {
    private var binding: FragmentEcomEventsBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnProductViewed.setOnClickListener { navigateTo(R.id.ecomEvents_to_ProductViewed) }
        binding!!.btnProductCategoryViewed.setOnClickListener { navigateTo(R.id.ecomEvents_to_ProductCategoryViewed) }
        binding!!.btnProductAddedToWishlist.setOnClickListener { navigateTo(R.id.ecomEvents_to_ProductAddedToWishlist) }
        binding!!.btnCartUpdated.setOnClickListener { navigateTo(R.id.ecomEvents_to_CartUpdated) }
        binding!!.btnOrderCreated.setOnClickListener { navigateTo(R.id.ecomEvents_to_OrderCreated) }
        binding!!.btnOrderUpdated.setOnClickListener { navigateTo(R.id.ecomEvents_to_OrderUpdated) }
        binding!!.btnOrderDelivered.setOnClickListener { navigateTo(R.id.ecomEvents_to_OrderDelivered) }
        binding!!.btnOrderCancelled.setOnClickListener { navigateTo(R.id.ecomEvents_to_OrderCancelled) }
        binding!!.btnSearchRequest.setOnClickListener { navigateTo(R.id.ecomEvents_to_SearchRequest) }
    }

    private fun navigateTo(fragmentId: Int) {
        NavHostFragment.findNavController(this).navigate(fragmentId)
    }
}