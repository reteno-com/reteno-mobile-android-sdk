package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.EcomEvent.OrderUpdated
import com.reteno.core.domain.model.ecom.Order
import com.reteno.sample.databinding.FragmentEcomEventsOrderUpsertBinding
import com.reteno.sample.util.Util

class FragmentEcomEventsOrderUpdated : FragmentEcomEventsOrderCreated() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsOrderUpsertBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun logEvent(order: Order?) {
        val ecomEvent: EcomEvent = OrderUpdated(
            order!!, Util.getTextOrNull(
                binding!!.etCurrencyCode
            )
        )
        reteno.logEcommerceEvent(ecomEvent)
    }
}