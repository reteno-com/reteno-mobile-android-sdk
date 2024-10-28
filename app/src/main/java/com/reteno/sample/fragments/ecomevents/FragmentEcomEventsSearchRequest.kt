package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.EcomEvent.SearchRequest
import com.reteno.sample.databinding.FragmentEcomEventsSearchRequestBinding
import com.reteno.sample.util.Util

class FragmentEcomEventsSearchRequest : BaseEcomEventsFragment() {
    private var binding: FragmentEcomEventsSearchRequestBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsSearchRequestBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener {
            val search = Util.getTextOrNull(
                binding!!.etSearch
            )
            if (search == null) {
                Toast.makeText(
                    requireContext(),
                    "ERROR. Required fields are empty or null",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val ecomEvent: EcomEvent = SearchRequest(search, binding!!.cbIsFound.isChecked)
            reteno.logEcommerceEvent(ecomEvent)
            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
        }
    }
}