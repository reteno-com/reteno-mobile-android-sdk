package com.reteno.sample.fragments.ecomevents

import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.EcomEvent.OrderCreated
import com.reteno.core.domain.model.ecom.Order
import com.reteno.core.domain.model.ecom.OrderItem
import com.reteno.core.domain.model.ecom.OrderStatus
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentEcomEventsOrderUpsertBinding
import com.reteno.sample.util.Util
import java.time.ZonedDateTime

open class FragmentEcomEventsOrderCreated : BaseEcomEventsFragment() {
    @JvmField
    protected var binding: FragmentEcomEventsOrderUpsertBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEcomEventsOrderUpsertBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.spnStatus.setAdapter(
            ArrayAdapter(
                view.context,
                android.R.layout.simple_spinner_item,
                OrderStatus.values()
            )
        )
        binding!!.spnStatus.setSelection(0)
        binding!!.etDate.setText(ZonedDateTime.now().toString())
        initListeners()
    }

    private fun initListeners() {
        binding!!.btnSubmit.setOnClickListener { v: View? ->
            val externalOrderId = Util.getTextOrNull(
                binding!!.etExternalOrderId
            )
            val totalCost = Util.getTextOrNull(binding!!.etTotalCost)
            val orderStatus = OrderStatus.valueOf(binding!!.spnStatus.getSelectedItem().toString())
            val date = Util.getTextOrNull(binding!!.etDate)
            if (externalOrderId == null || totalCost == null || date == null) {
                Toast.makeText(
                    requireContext(),
                    "ERROR. Required fields are empty or null",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val shippingString = Util.getTextOrNull(binding!!.etShipping)
            var shipping: Double? = null
            if (shippingString != null) {
                shipping = shippingString.toDouble()
            }
            val discountString = Util.getTextOrNull(binding!!.etDiscount)
            var discount: Double? = null
            if (discountString != null) {
                discount = discountString.toDouble()
            }
            val taxesString = Util.getTextOrNull(binding!!.etTaxes)
            var taxes: Double? = null
            if (taxesString != null) {
                taxes = taxesString.toDouble()
            }
            val orderItems = getOrderItems(binding!!.llOrderItems)
            val orderBuilder = Order.Builder(
                externalOrderId,
                Util.getTextOrNull(binding!!.etExternalCustomerId), totalCost.toDouble(),
                orderStatus,
                ZonedDateTime.parse(date)
            )
            orderBuilder.cartId = Util.getTextOrNull(binding!!.etCartId)
            orderBuilder.email = Util.getTextOrNull(binding!!.etEmail)
            orderBuilder.phone = Util.getTextOrNull(binding!!.etPhone)
            orderBuilder.firstName = Util.getTextOrNull(binding!!.etFirstName)
            orderBuilder.lastName = Util.getTextOrNull(binding!!.etLastName)
            orderBuilder.shipping = shipping
            orderBuilder.discount = discount
            orderBuilder.taxes = taxes
            orderBuilder.restoreUrl = Util.getTextOrNull(binding!!.etRestoreUrl)
            orderBuilder.statusDescription = Util.getTextOrNull(binding!!.etStatusDescription)
            orderBuilder.storeId = Util.getTextOrNull(binding!!.etStoreId)
            orderBuilder.source = Util.getTextOrNull(binding!!.etSource)
            orderBuilder.deliveryMethod = Util.getTextOrNull(binding!!.etDeliveryMethod)
            orderBuilder.paymentMethod = Util.getTextOrNull(binding!!.etPaymentMethod)
            orderBuilder.deliveryAddress = Util.getTextOrNull(binding!!.etDeliveryAddress)
            orderBuilder.items = orderItems
            orderBuilder.attributes = getOrderCustomAttributes(binding!!.llCustomAttributes)
            logEvent(orderBuilder.build())
            Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
        }
        binding!!.btnCustomAttributePlus.setOnClickListener {
            val view = createNewFields(binding!!, binding!!.llCustomAttributes)
            binding!!.llCustomAttributes.addView(view)
        }
        binding!!.btnCustomAttributeMinus.setOnClickListener {
            val countView = binding!!.llCustomAttributes.childCount
            if (countView > 0) {
                binding!!.llCustomAttributes.removeViewAt(countView - 1)
            }
        }
        binding!!.btnOrderItemsPlus.setOnClickListener {
            val view = createNewOrderItem(binding!!, binding!!.llOrderItems)
            binding!!.llOrderItems.addView(view)
        }
        binding!!.btnOrderItemsMinus.setOnClickListener {
            val countView = binding!!.llOrderItems.childCount
            if (countView > 0) {
                binding!!.llOrderItems.removeViewAt(countView - 1)
            }
        }
    }

    protected open fun logEvent(order: Order?) {
        val ecomEvent: EcomEvent = OrderCreated(
            order!!, Util.getTextOrNull(
                binding!!.etCurrencyCode
            )
        )
        reteno.logEcommerceEvent(ecomEvent)
    }

    private fun createNewOrderItem(viewBinding: ViewBinding, container: LinearLayout): View {
        val layoutInflater = LayoutInflater.from(viewBinding.root.context)
        return layoutInflater.inflate(R.layout.view_ecom_events_order_items, container, false)
    }

    private fun getOrderItems(llOrderItems: LinearLayout): List<OrderItem>? {
        val countView = llOrderItems.childCount
        if (countView == 0) return null
        val list: MutableList<OrderItem> = ArrayList()
        for (i in 0 until countView) {
            val parent = llOrderItems.getChildAt(i) as LinearLayout
            val etExternalItemId = parent.getChildAt(0) as EditText
            val etName = parent.getChildAt(1) as EditText
            val etCategory = parent.getChildAt(2) as EditText
            val etQuantity = parent.getChildAt(3) as EditText
            val etCost = parent.getChildAt(4) as EditText
            val etUrl = parent.getChildAt(5) as EditText
            val etImageUrl = parent.getChildAt(6) as EditText
            val etDescription = parent.getChildAt(7) as EditText
            val externalItemId = Util.getTextOrNull(etExternalItemId)
            val name = Util.getTextOrNull(etName)
            val category = Util.getTextOrNull(etCategory)
            val quantity = Util.getTextOrNull(etQuantity)
            val cost = Util.getTextOrNull(etCost)
            val url = Util.getTextOrNull(etUrl)
            val imageUrl = Util.getTextOrNull(etImageUrl)
            val description = Util.getTextOrNull(etDescription)
            if (externalItemId != null && name != null && category != null && quantity != null && cost != null && url != null) {
                list.add(
                    OrderItem(
                        externalItemId,
                        name,
                        category, quantity.toDouble(), cost.toDouble(),
                        url,
                        imageUrl,
                        description
                    )
                )
            }
        }
        return list
    }

    private fun getOrderCustomAttributes(llCustomAttributes: LinearLayout): List<Pair<String, String>>? {
        val countView = llCustomAttributes.childCount
        if (countView == 0) return null
        val list: MutableList<Pair<String, String>> = ArrayList()
        for (i in 0 until countView) {
            val parent = llCustomAttributes.getChildAt(i) as LinearLayout
            val etKey = parent.getChildAt(0) as EditText
            val etValue = parent.getChildAt(1) as EditText
            val key = Util.getTextOrNull(etKey)
            val value = Util.getTextOrNull(etValue)
            if (key != null && value != null) {
                list.add(Pair(key, value))
            }
        }
        return list
    }
}