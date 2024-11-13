package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.ProductCategoryView
import com.reteno.sample.databinding.FragmentEcomEventsProductCategoryViewedBinding
import com.reteno.sample.util.Util

class FragmentEcomEventsProductCategoryViewed : BaseEcomEventsFragment() {

    private var binding: FragmentEcomEventsProductCategoryViewedBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsProductCategoryViewedBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val productCategoryId = Util.getTextOrNull(
                binding!!.etProductCategoryId
            )
            if (productCategoryId == null) {
                Toast.makeText(
                    requireContext(),
                    "ERROR. Required fields are empty or null",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val productCategoryView = ProductCategoryView(
                productCategoryId,
                getCustomAttributes(binding!!.llCustomAttributes)
            )
            val ecomEvent: EcomEvent = EcomEvent.ProductCategoryViewed(productCategoryView)
            reteno.logEcommerceEvent(ecomEvent)
            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
        }
        binding!!.btnCustomAttributePlus.setOnClickListener { v: View? ->
            val view = createNewFields(binding!!, binding!!.llCustomAttributes)
            binding!!.llCustomAttributes.addView(view)
        }
        binding!!.btnCustomAttributeMinus.setOnClickListener { v: View? ->
            val countView = binding!!.llCustomAttributes.childCount
            if (countView > 0) {
                binding!!.llCustomAttributes.removeViewAt(countView - 1)
            }
        }
    }
}