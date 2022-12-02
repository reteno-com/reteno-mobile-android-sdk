package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.event.EventDb
import com.reteno.core.data.local.model.event.ParameterDb
import com.reteno.core.data.remote.mapper.RemoteConstants
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.ecom.ProductCategoryView
import com.reteno.core.domain.model.ecom.ProductInCard
import com.reteno.core.domain.model.ecom.ProductView
import com.reteno.core.util.Util.formatToRemote
import com.reteno.core.util.escape
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
            put(RemoteConstants.EcomEvent.PRICE, price.toString())
            put(RemoteConstants.EcomEvent.IS_IN_STOCK, isInStock.toIntValue())

            attributes?.forEach {
                put(it.name, it.value)
            }
        }

        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.PRODUCT, event.product.formatToValue().escape()))

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

            attributes?.forEach {
                put(it.name, it.value)
            }
        }

        return jsonObject.toString()
    }

    return listOf(ParameterDb(RemoteConstants.EcomEvent.CATEGORY, event.category.formatToValue().escape()))
}

private fun EcomEvent.ProductAddedToWishlist.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductView.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(RemoteConstants.EcomEvent.PRODUCT_ID, productId)
            put(RemoteConstants.EcomEvent.PRICE, price.toString())
            put(RemoteConstants.EcomEvent.IS_IN_STOCK, isInStock.toIntValue())

            attributes?.forEach {
                put(it.name, it.value)
            }
        }

        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.PRODUCT, event.product.formatToValue().escape()))

        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
        }
    }
}

private fun EcomEvent.CartUpdated.formatToEventParams(): List<ParameterDb> {
    val event = this

    fun ProductInCard.formatToValue(): String {
        val jsonObject = JSONObject().apply {
            put(RemoteConstants.EcomEvent.PRODUCT_ID, productId)
            put(RemoteConstants.EcomEvent.QUANTITY, quantity.toString())
            put(RemoteConstants.EcomEvent.PRICE, price.toString())

            discount?.let {
                put(RemoteConstants.EcomEvent.DISCOUNT, it.toString())
            }

            attributes?.forEach {
                put(it.name, it.value)
            }
        }

        return jsonObject.toString()
    }

    return buildList {
        add(ParameterDb(RemoteConstants.EcomEvent.CART_ID, event.cardId))
        add(
            ParameterDb(
                RemoteConstants.EcomEvent.PRODUCTS,
                event.products.joinToString { it.formatToValue() }.escape()
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
        add(ParameterDb(RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID, event.externalOrderId))
        add(ParameterDb(RemoteConstants.EcomEvent.TOTAL_COST, event.totalCost.toString()))
        add(ParameterDb(RemoteConstants.EcomEvent.STATUS, event.status.name))
        add(ParameterDb(RemoteConstants.EcomEvent.DATE, event.data.formatToRemote()))

        event.cartId?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CART_ID, it))
        }

        event.currencyCode?.let {
            add(ParameterDb(RemoteConstants.EcomEvent.CURRENCY_CODE, it))
        }

        event.attributes?.forEach {
            add(ParameterDb(it.name, it.value.toString()))
        }
    }
}

private fun EcomEvent.OrderUpdated.formatToEventParams(): List<ParameterDb> {
    return listOf(ParameterDb(RemoteConstants.EcomEvent.EXTERNAL_ORDER_ID, externalOrderId))
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
