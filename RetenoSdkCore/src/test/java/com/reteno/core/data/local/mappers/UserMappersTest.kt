package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class UserMappersTest {

    @Test
    fun `should store user of a device with every attribute`() = runUnitTest {
        given()
        val user = User(
            userAttributes = UserAttributes(
                phone = "+380501234567",
                email = "ann@example.com",
                firstName = "Ann",
                lastName = "Lee",
                languageCode = "uk",
                timeZone = "Europe/Kyiv",
                address = Address(region = "Kyiv", town = "Kyiv", address = "Main st 1", postcode = "01001"),
                fields = listOf(UserCustomField("plan", "pro")),
                marketId = "m-1"
            ),
            subscriptionKeys = listOf("news"),
            groupNamesInclude = listOf("vip"),
            groupNamesExclude = listOf("churned")
        )

        whenn()
        val stored = user.toDb(stubDeviceId())

        then()
        val expectedAttributes = UserAttributesDb(
            phone = "+380501234567",
            email = "ann@example.com",
            firstName = "Ann",
            lastName = "Lee",
            languageCode = "uk",
            timeZone = "Europe/Kyiv",
            address = AddressDb(region = "Kyiv", town = "Kyiv", address = "Main st 1", postcode = "01001"),
            fields = listOf(UserCustomFieldDb("plan", "pro")),
            marketId = "m-1"
        )
        assertEquals(
            listOf(
                "device-body",
                "external-user",
                expectedAttributes,
                listOf("news"),
                listOf("vip"),
                listOf("churned")
            ),
            listOf(
                stored.deviceId,
                stored.externalUserId,
                stored.userAttributes,
                stored.subscriptionKeys,
                stored.groupNamesInclude,
                stored.groupNamesExclude
            )
        )
    }

    @Test
    fun `should store user without attributes`() = runUnitTest {
        given()
        val user = User(subscriptionKeys = listOf("news"))

        whenn()
        val stored = user.toDb(stubDeviceId())

        then()
        assertNull(stored.userAttributes)
    }

    @Test
    fun `should store attributes without address and custom fields`() = runUnitTest {
        given()
        val attributes = UserAttributes(firstName = "Ann")

        whenn()
        val stored = attributes.toDb()

        then()
        assertEquals(UserAttributesDb(firstName = "Ann"), stored)
    }
}
