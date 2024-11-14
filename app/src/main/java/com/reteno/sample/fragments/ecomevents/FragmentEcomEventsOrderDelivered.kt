package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.EcomEvent.OrderDelivered
import com.reteno.sample.databinding.FragmentEcomEventsOrderDeliveredBinding
import com.reteno.sample.util.Util

class FragmentEcomEventsOrderDelivered : BaseEcomEventsFragment() {
    private var binding: FragmentEcomEventsOrderDeliveredBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsOrderDeliveredBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val externalOrderId = Util.getTextOrNull(
                binding!!.etExternalOrderId
            )
            if (externalOrderId == null) {
                Toast.makeText(
                    requireContext(),
                    "ERROR. Required fields are empty or null",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val ecomEvent: EcomEvent = OrderDelivered(externalOrderId)
            reteno.logEcommerceEvent(ecomEvent)
            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
        }
    }
}