package com.reteno.core.domain.model.ecom

import android.util.Pair
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class OrderTest {

    @Test
    fun `should build order with every detail set on the builder`() = runUnitTest {
        given()
        val builder = Order.Builder("order-1", "customer-1", 99.5, OrderStatus.DELIVERED, DATE).apply {
            cartId = "cart-1"
            email = "a@b.c"
            phone = "+100"
            firstName = "Ann"
            lastName = "Lee"
            shipping = 5.0
            discount = 2.5
            taxes = 1.0
            restoreUrl = "https://restore"
            statusDescription = "delivered"
            storeId = "store-1"
            source = "app"
            deliveryMethod = "courier"
            paymentMethod = "card"
            deliveryAddress = "Main st 1"
            items = listOf(ITEM)
            attributes = listOf(ATTRIBUTE)
        }

        whenn()
        val order = builder.build()

        then()
        val expected = listOf(
            "order-1", "customer-1", 99.5, OrderStatus.DELIVERED, DATE, "cart-1", "a@b.c", "+100", "Ann", "Lee",
            5.0, 2.5, 1.0, "https://restore", "delivered", "store-1", "app", "courier", "card", "Main st 1",
            listOf(ITEM), listOf(ATTRIBUTE)
        )
        assertEquals(expected, order.details())
        assertEquals(expected, builder.details())
    }

    @Test
    fun `should describe an order item for the host`() = runUnitTest {
        given()
        val item = ITEM

        whenn()
        val details = listOf(
            item.externalItemId, item.name, item.category, item.quantity, item.cost, item.url, item.imageUrl,
            item.description
        )

        then()
        assertEquals(listOf("item-1", "Boot", "shoes", 1.0, 99.5, "https://item", "https://img", "leather"), details)
    }

    private fun Order.details(): List<Any?> = listOf(
        externalOrderId, externalCustomerId, totalCost, status, date, cartId, email, phone, firstName, lastName,
        shipping, discount, taxes, restoreUrl, statusDescription, storeId, source, deliveryMethod, paymentMethod,
        deliveryAddress, items, attributes
    )

    private fun Order.Builder.details(): List<Any?> = listOf(
        externalOrderId, externalCustomerId, totalCost, status, date, cartId, email, phone, firstName, lastName,
        shipping, discount, taxes, restoreUrl, statusDescription, storeId, source, deliveryMethod, paymentMethod,
        deliveryAddress, items, attributes
    )

    private companion object {
        val DATE: ZonedDateTime = ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC)
        val ITEM = OrderItem("item-1", "Boot", "shoes", 1.0, 99.5, "https://item", "https://img", "leather")
        val ATTRIBUTE: Pair<String, String> = Pair("gift", "yes")
    }
}
