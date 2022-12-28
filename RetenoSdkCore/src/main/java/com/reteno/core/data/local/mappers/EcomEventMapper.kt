package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.domain.model.ecom.RemoteConstants
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.domain.model.ecom.*
import com.reteno.core.util.Util.formatToRemote
import org.json.JSONArray
import org.json.JSONObject

fun EcomEvent.toDb() = EventDb(
    eventTypeKey = getTypeKey(),
    occurred = occurred.formatToRemote(),
    params = convertToParams()
)

private fun EcomEvent.getTypeKey(): String {
    return when (this) {
        is EcomEvent.ProductViewed -> RemoteConstants.EcomEvent.EVENT_TYPE_PRODUCT_VIEWED
        is EcomEvent.ProductCategoryViewed -> RemoteConstants.EcomEvent.EVENT_TYPE_PRODUCT_CATEGORY_VIEWED
        is EcomEvent.ProductAddedToWishlist -> RemoteConstants.EcomEvent.EVENT_TYPE_PRODUCT_ADDED_TO_WISHLIST
        is EcomEvent.CartUpdated -> RemoteConstants.EcomEvent.EVENT_TYPE_CART_UPDATED
        is EcomEvent.OrderCreated -> RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_CREATED
        is EcomEvent.OrderUpdated -> RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_UPDATED
        is EcomEvent.OrderDelivered -> RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_DELIVERED
        is EcomEvent.OrderCancelled -> RemoteConstants.EcomEvent.EVENT_TYPE_ORDER_CANCELLED
        is EcomEvent.SearchRequest -> RemoteConstants.EcomEvent.EVENT_TYPE_SEARCH_REQUEST
    }
}

private fun EcomEvent.convertToParams(): List<ParameterDb> {
    return when (this) {
        is EcomEvent.ProductViewed -> formatToEventParams()
        is EcomEvent.ProductCategoryViewed -> formatToEventParams()
        is EcomEvent.ProductAddedToWishlist -> formatToEventParams()
        is EcomEvent.CartUpdated -> formatToEventParams()
        is EcomEvent.OrderCreated -> formatToEventParams()
        is EcomEvent.OrderUpdated -> formatToEventParams()
        is EcomEvent.OrderDelivered -> formatToEventParams()
        is EcomEvent.OrderCancelled -> formatToEventParams()
        is EcomEvent.SearchRequest -> formatToEventParams()
    }
}

private fun EcomEvent.ProductViewed.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(RemoteConstants.EcomEvent.PRODUCT_ID, productId)
            put(RemoteConstants.EcomEvent.PRICE, price)
            put(RemoteConstants.EcomEvent.IS_IN_STOCK, isInStock.toIntValue())

            attributes?.forEach {
                put(it.name, it.value)
            }
        }

        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.PRODUCT, event.product.formatToValue()))

        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
        }
    }
}

private fun EcomEvent.ProductCategoryViewed.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductCategoryView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(RemoteConstants.EcomEvent.PRODUCT_CATEGORY_ID, productCategoryId)

            attributes?.forEach(::putAttributes)
        }

        return jsonObject.toString()
    }

    return listOf(ParameterDb(RemoteConstants.EcomEvent.CATEGORY, event.category.formatToValue()))
}

private fun EcomEvent.ProductAddedToWishlist.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(RemoteConstants.EcomEvent.PRODUCT_ID, productId)
            put(RemoteConstants.EcomEvent.PRICE, price)
            put(RemoteConstants.EcomEvent.IS_IN_STOCK, isInStock.toIntValue())

            attributes?.forEach(::putAttributes)
        }

        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.PRODUCT, event.product.formatToValue()))

        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
        }
    }
}

private fun EcomEvent.CartUpdated.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductInCart.formatToValue(): JSONObject {
        val jsonObject = JSONObject().apply {
            put(RemoteConstants.EcomEvent.PRODUCT_ID, productId)
            put(RemoteConstants.EcomEvent.QUANTITY, quantity.toString())
            put(RemoteConstants.EcomEvent.PRICE, price)

            name?.let {
                put(RemoteConstants.EcomEvent.PRODUCT_NAME, it)
            }
            category?.let {
                put(RemoteConstants.EcomEvent.PRODUCT_CATEGORY, it)
            }
            discount?.let {
                put(RemoteConstants.EcomEvent.DISCOUNT, it.toString())
            }

            attributes?.forEach(::putAttributes)
        }

        return jsonObject
    }

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.CART_ID, event.cartId))
        add(
            ParameterDb(
                RemoteConstants.EcomEvent.PRODUCTS,
                JSONArray(event.products.map { it.formatToValue() }).toString()
            )
        )

        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
        }
    }
}

private fun EcomEvent.OrderCreated.formatToEventParams(): List<ParameterDb> {
    val event = this

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID, event.order.externalOrderId))
        add(
            ParameterDb(
                RemoteConstants.EcomEvent.EXTERNAL_CUSTOMER_ID,
                event.order.externalCustomerId
            )
        )
        add(ParameterDb(RemoteConstants.EcomEvent.TOTAL_COST, event.order.totalCost.toString()))
        add(ParameterDb(RemoteConstants.EcomEvent.STATUS, event.order.status.name))
        add(ParameterDb(RemoteConstants.EcomEvent.DATE, event.order.date.formatToRemote()))

        event.order.cartId?.let { add(ParameterDb(RemoteConstants.EcomEvent.CART_ID, it)) }
        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY, it))
        }

        event.order.email?.let { add(ParameterDb(RemoteConstants.EcomEvent.EMAIL, it)) }
        event.order.phone?.let { add(ParameterDb(RemoteConstants.EcomEvent.PHONE, it)) }
        event.order.firstName?.let { add(ParameterDb(RemoteConstants.EcomEvent.FIRST_NAME, it)) }
        event.order.lastName?.let { add(ParameterDb(RemoteConstants.EcomEvent.LAST_NAME, it)) }
        event.order.shipping?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.SHIPPING,
                    it.toString()
                )
            )
        }
        event.order.discount?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.ORDER_DISCOUNT,
                    it.toString()
                )
            )
        }
        event.order.taxes?.let { add(ParameterDb(RemoteConstants.EcomEvent.TAXES, it.toString())) }
        event.order.restoreUrl?.let { add(ParameterDb(RemoteConstants.EcomEvent.RESTORE_URL, it)) }
        event.order.statusDescription?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.STATUS_DESCRIPTION,
                    it
                )
            )
        }
        event.order.storeId?.let { add(ParameterDb(RemoteConstants.EcomEvent.STORE_ID, it)) }
        event.order.source?.let { add(ParameterDb(RemoteConstants.EcomEvent.SOURCE, it)) }
        event.order.deliveryMethod?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.DELIVERY_METHOD,
                    it
                )
            )
        }
        event.order.paymentMethod?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.PAYMENT_METHOD,
                    it
                )
            )
        }
        event.order.deliveryAddress?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.DELIVERY_ADDRESS,
                    it
                )
            )
        }
        event.order.items?.let { add(ParameterDb(RemoteConstants.EcomEvent.ITEMS, it.toJson())) }

        event.order.attributes?.forEach { add(ParameterDb(it.name, it.value.toJson())) }
    }
}

private fun EcomEvent.OrderUpdated.formatToEventParams(): List<ParameterDb> {
    val event = this

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID, event.order.externalOrderId))
        add(
            ParameterDb(
                RemoteConstants.EcomEvent.EXTERNAL_CUSTOMER_ID,
                event.order.externalCustomerId
            )
        )
        add(ParameterDb(RemoteConstants.EcomEvent.TOTAL_COST, event.order.totalCost.toString()))
        add(ParameterDb(RemoteConstants.EcomEvent.STATUS, event.order.status.name))
        add(ParameterDb(RemoteConstants.EcomEvent.DATE, event.order.date.formatToRemote()))

        event.order.cartId?.let { add(ParameterDb(RemoteConstants.EcomEvent.CART_ID, it)) }
        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY, it))
        }

        event.order.email?.let { add(ParameterDb(RemoteConstants.EcomEvent.EMAIL, it)) }
        event.order.phone?.let { add(ParameterDb(RemoteConstants.EcomEvent.PHONE, it)) }
        event.order.firstName?.let { add(ParameterDb(RemoteConstants.EcomEvent.FIRST_NAME, it)) }
        event.order.lastName?.let { add(ParameterDb(RemoteConstants.EcomEvent.LAST_NAME, it)) }
        event.order.shipping?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.SHIPPING,
                    it.toString()
                )
            )
        }
        event.order.discount?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.ORDER_DISCOUNT,
                    it.toString()
                )
            )
        }
        event.order.taxes?.let { add(ParameterDb(RemoteConstants.EcomEvent.TAXES, it.toString())) }
        event.order.restoreUrl?.let { add(ParameterDb(RemoteConstants.EcomEvent.RESTORE_URL, it)) }
        event.order.statusDescription?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.STATUS_DESCRIPTION,
                    it
                )
            )
        }
        event.order.storeId?.let { add(ParameterDb(RemoteConstants.EcomEvent.STORE_ID, it)) }
        event.order.source?.let { add(ParameterDb(RemoteConstants.EcomEvent.SOURCE, it)) }
        event.order.deliveryMethod?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.DELIVERY_METHOD,
                    it
                )
            )
        }
        event.order.paymentMethod?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.PAYMENT_METHOD,
                    it
                )
            )
        }
        event.order.deliveryAddress?.let {
            add(
                ParameterDb(
                    RemoteConstants.EcomEvent.DELIVERY_ADDRESS,
                    it
                )
            )
        }
        event.order.items?.let { add(ParameterDb(RemoteConstants.EcomEvent.ITEMS, it.toJson())) }

        event.order.attributes?.forEach { add(ParameterDb(it.name, it.value.toJson())) }
    }
}

private fun EcomEvent.OrderDelivered.formatToEventParams(): List<ParameterDb> {
    return listOf(ParameterDb(RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID, externalOrderId))
}

private fun EcomEvent.OrderCancelled.formatToEventParams(): List<ParameterDb> {
    return listOf(ParameterDb(RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID, externalOrderId))
}

private fun EcomEvent.SearchRequest.formatToEventParams(): List<ParameterDb> {
    return listOf(
        ParameterDb(RemoteConstants.EcomEvent.SEARCH, search),
        ParameterDb(RemoteConstants.EcomEvent.IS_FOUND, isFound.toIntValue().toString())
    )
}

private fun JSONObject.putAttributes(attrs: Attributes) {
    val jsonArray = JSONArray()
    for (element in attrs.value) {
        jsonArray.put(element)
    }
    put(attrs.name, jsonArray)
}
