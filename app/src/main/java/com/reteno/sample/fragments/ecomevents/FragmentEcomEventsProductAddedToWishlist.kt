package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.EcomEvent.ProductAddedToWishlist
import com.reteno.core.domain.model.ecom.ProductView
import com.reteno.sample.databinding.FragmentEcomEventsProductAddedToWishlistBinding
import com.reteno.sample.util.Util

class FragmentEcomEventsProductAddedToWishlist : BaseEcomEventsFragment() {
    private var binding: FragmentEcomEventsProductAddedToWishlistBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsProductAddedToWishlistBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val productId = Util.getTextOrNull(
                binding!!.etProductId
            )
            val priceString = Util.getTextOrNull(binding!!.etPrice)
            var price: Double? = null
            if (priceString != null) {
                price = priceString.toDouble()
            }
            if (productId == null || price == null) {
                Toast.makeText(
                    requireContext(),
                    "ERROR. Required fields are empty or null",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val productView = ProductView(
                productId,
                price,
                binding!!.cbIsInStock.isChecked,
                getCustomAttributes(binding!!.llCustomAttributes)
            )
            val ecomEvent: EcomEvent = ProductAddedToWishlist(
                productView, Util.getTextOrNull(
                    binding!!.etCurrencyCode
                )
            )
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