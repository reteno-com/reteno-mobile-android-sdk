package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.EcomEvent.CartUpdated
import com.reteno.core.domain.model.ecom.ProductInCart
import com.reteno.sample.databinding.FragmentEcomEventsCartUpdatedBinding
import com.reteno.sample.databinding.ViewEcomEventsCartItemBinding
import com.reteno.sample.util.Util

class FragmentEcomEventsCartUpdated : BaseEcomEventsFragment() {
    private var binding: FragmentEcomEventsCartUpdatedBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsCartUpdatedBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnProductsPlus.setOnClickListener { v: View? ->
            val productBinding = createNewProduct(binding, binding!!.llProducts)
            setupSingleProductView(productBinding)
            binding!!.llProducts.addView(productBinding.getRoot())
        }
        binding!!.btnProductsMinus.setOnClickListener { v: View? ->
            val countView = binding!!.llProducts.childCount
            if (countView > 0) {
                binding!!.llProducts.removeViewAt(countView - 1)
            }
        }
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val cartId = Util.getTextOrNull(
                binding!!.etCartId
            )
            try {
                val products = getProducts(binding!!.llProducts)
                require(!(cartId == null || products == null)) { "ERROR. Cart ID should not be empty or null. Nothing done" }
                val ecomEvent: EcomEvent = CartUpdated(
                    cartId, products, Util.getTextOrNull(
                        binding!!.etCurrencyCode
                    )
                )
                reteno.logEcommerceEvent(ecomEvent)
                Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
            } catch (iae: IllegalArgumentException) {
                Toast.makeText(
                    requireContext(),
                    "ERROR. Required fields are empty or null",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createNewProduct(
        viewBinding: ViewBinding?,
        container: LinearLayout
    ): ViewEcomEventsCartItemBinding {
        val layoutInflater = LayoutInflater.from(viewBinding!!.root.context)
        return ViewEcomEventsCartItemBinding.inflate(layoutInflater, container, false)
    }

    private fun setupSingleProductView(productBinding: ViewEcomEventsCartItemBinding) {
        productBinding.btnCustomAttributePlus.setOnClickListener { v: View? ->
            val view = createNewFields(binding!!, productBinding.llCustomAttributes)
            productBinding.llCustomAttributes.addView(view)
        }
        productBinding.btnCustomAttributeMinus.setOnClickListener { v: View? ->
            val countView = productBinding.llCustomAttributes.childCount
            if (countView > 0) {
                productBinding.llCustomAttributes.removeViewAt(countView - 1)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun getProducts(llProductLayout: LinearLayout): List<ProductInCart>? {
        val countView = llProductLayout.childCount
        if (countView == 0) return null
        val list: MutableList<ProductInCart> = ArrayList()
        for (i in 0 until countView) {
            val parent = llProductLayout.getChildAt(i) as LinearLayout
            val etProductId = parent.getChildAt(0) as EditText
            val etQuantity = parent.getChildAt(1) as EditText
            val etPrice = parent.getChildAt(2) as EditText
            val etDiscount = parent.getChildAt(3) as EditText
            val etName = parent.getChildAt(4) as EditText
            val etCategory = parent.getChildAt(5) as EditText
            val llCustomAttributes = parent.getChildAt(7) as LinearLayout
            val productId = Util.getTextOrNull(etProductId)
            val quantityString = Util.getTextOrNull(etQuantity)
            var quantity: Int? = null
            if (quantityString != null) {
                quantity = quantityString.toInt()
            }
            val priceString = Util.getTextOrNull(etPrice)
            var price: Double? = null
            if (priceString != null) {
                price = priceString.toDouble()
            }
            val discountString = Util.getTextOrNull(etDiscount)
            var discount: Double? = null
            if (discountString != null) {
                discount = discountString.toDouble()
            }
            val productName = Util.getTextOrNull(etName)
            val productCategory = Util.getTextOrNull(etCategory)
            require(!(productId == null || quantity == null || price == null)) { "ERROR. Required fields are empty or null" }
            val productInCart = ProductInCart(
                productId,
                quantity,
                price,
                discount,
                productName,
                productCategory,
                getCustomAttributes(llCustomAttributes)
            )
            list.add(productInCart)
        }
        return list
    }
}