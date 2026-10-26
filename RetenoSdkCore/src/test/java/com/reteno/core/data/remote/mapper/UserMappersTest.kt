package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.fixtures.stubUserAttributesDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class UserMappersTest {

    @Test
    fun `should send every stored user field`() = runUnitTest {
        given()
        val user = fullUser()

        whenn()
        val remote = user.toRemote()

        then()
        val attributes = requireNotNull(remote.userAttributes)
        val address = requireNotNull(attributes.address)
        val field = requireNotNull(attributes.fields).single()
        val expected = listOf(
            "device-body", "external-user", listOf("news"), listOf("vip"), listOf("churned"),
            "+380501234567", "ann@example.com", "Ann", "Lee", "uk", "Europe/Kyiv", "m-1",
            "Kyiv", "Kyiv", "Main st 1", "01001", "plan", "pro"
        )
        val actual = listOf(
            remote.deviceId, remote.externalUserId, remote.subscriptionKeys, remote.groupNamesInclude,
            remote.groupNamesExclude, attributes.phone, attributes.email, attributes.firstName, attributes.lastName,
            attributes.languageCode, attributes.timeZone, attributes.marketId, address.region, address.town,
            address.address, address.postcode, field.key, field.value
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `should read sent user back as stored user`() = runUnitTest {
        given()
        val user = fullUser()

        whenn()
        val stored = user.toRemote().toDb()

        then()
        assertEquals(user.copy(rowId = null, createdAt = 0L), stored)
    }

    @Test
    fun `should send user without attributes address and fields`() = runUnitTest {
        given()
        val user = UserDb(
            deviceId = "device-body",
            userAttributes = stubUserAttributesDb().copy(address = null, fields = null)
        )

        whenn()
        val remote = user.toRemote()

        then()
        assertNull(remote.userAttributes?.address)
        assertNull(remote.userAttributes?.fields)
    }

    @Test
    fun `should read user without attributes address and fields back`() = runUnitTest {
        given()
        val users = listOf(
            UserDb(deviceId = "device-body"),
            UserDb(
                deviceId = "device-body",
                userAttributes = stubUserAttributesDb().copy(address = null, fields = null)
            )
        )

        whenn()
        val stored = users.map { it.toRemote().toDb() }

        then()
        assertEquals(users, stored)
    }

    private fun fullUser() = UserDb(
        deviceId = "device-body",
        externalUserId = "external-user",
        userAttributes = stubUserAttributesDb(),
        subscriptionKeys = listOf("news"),
        groupNamesInclude = listOf("vip"),
        groupNamesExclude = listOf("churned")
    )
}
