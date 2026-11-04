package com.reteno.core.data.remote.model.user

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class UserRemoteTest {

    @Test
    fun `should send whole user when nothing was sent before`() = runUnitTest {
        given()
        val user = full()

        whenn()
        val diff = user.createDiffModel(null)

        then()
        assertEquals(user, diff)
    }

    @Test
    fun `should send nothing when user did not change`() = runUnitTest {
        given()
        val user = full()

        whenn()
        val diff = user.createDiffModel(full())

        then()
        assertNull(diff)
    }

    @Test
    fun `should send only changed attributes and no unchanged lists`() = runUnitTest {
        given()
        val older = full()

        whenn()
        val diff = full().copy(userAttributes = UserAttributesRemote(firstName = "Olena")).createDiffModel(older)

        then()
        assertEquals(
            UserRemote(
                deviceId = "device-1",
                externalUserId = "user-1",
                userAttributes = UserAttributesRemote(firstName = "Olena")
            ),
            diff
        )
    }

    @Test
    fun `should send every list that changed`() = runUnitTest {
        given()
        val older = full().copy(
            subscriptionKeys = null,
            groupNamesInclude = listOf("old"),
            groupNamesExclude = listOf("a", "b")
        )

        whenn()
        val diff = requireNotNull(full().copy(userAttributes = null).createDiffModel(older))

        then()
        assertEquals(
            listOf(listOf("news"), listOf("vip"), listOf("churned"), null),
            listOf(diff.subscriptionKeys, diff.groupNamesInclude, diff.groupNamesExclude, diff.userAttributes)
        )
    }

    @Test
    fun `should treat lists with the same items in another order as unchanged`() = runUnitTest {
        given()
        val older = full().copy(subscriptionKeys = listOf("promo", "news"))

        whenn()
        val diff = full().copy(subscriptionKeys = listOf("news", "promo")).createDiffModel(older)

        then()
        assertNull(diff)
    }

    @Test
    fun `should send nothing when only the user identity differs`() = runUnitTest {
        given()
        val older = full().copy(externalUserId = "user-0")

        whenn()
        val diff = full().createDiffModel(older)

        then()
        assertNull(diff)
    }

    @Test
    fun `should treat a list removed now as changed`() = runUnitTest {
        given()
        val older = full()

        whenn()
        val diff = full().copy(groupNamesExclude = null).createDiffModel(older)

        then()
        assertEquals(UserRemote(deviceId = "device-1", externalUserId = "user-1"), diff)
    }

    @Test
    fun `should keep user data sent before for parts not set now`() = runUnitTest {
        given()
        val older = full()

        whenn()
        val accumulated = UserRemote(
            deviceId = "device-1",
            externalUserId = "user-1",
            userAttributes = UserAttributesRemote(firstName = "Olena")
        )
            .createAccModel(older)

        then()
        assertEquals(
            full().copy(userAttributes = requireNotNull(full().userAttributes).copy(firstName = "Olena")),
            accumulated
        )
    }

    @Test
    fun `should keep lists set now over those sent before`() = runUnitTest {
        given()
        val older = full().copy(subscriptionKeys = listOf("old"), userAttributes = null)

        whenn()
        val accumulated = full().copy(userAttributes = null).createAccModel(older)

        then()
        assertEquals(full().copy(userAttributes = null), accumulated)
    }

    @Test
    fun `should accumulate nothing new from equal or absent user`() = runUnitTest {
        given()
        val user = full()

        whenn()
        val accumulated = listOf(user.createAccModel(null), user.createAccModel(full()))

        then()
        assertEquals(listOf(user, user), accumulated)
    }

    private fun full() = UserRemote(
        deviceId = "device-1",
        externalUserId = "user-1",
        userAttributes = UserAttributesRemote(firstName = "Ann", email = "ann@example.com"),
        subscriptionKeys = listOf("news"),
        groupNamesInclude = listOf("vip"),
        groupNamesExclude = listOf("churned")
    )
}
