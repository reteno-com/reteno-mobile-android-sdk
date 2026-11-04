package com.reteno.core.data.remote.model.user

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class UserAttributesRemoteTest {

    @Test
    fun `should send every attribute when nothing was sent before`() = runUnitTest {
        given()
        val attributes = full()

        whenn()
        val diff = attributes.createDiffModel(null)

        then()
        assertEquals(attributes, diff)
    }

    @Test
    fun `should send nothing when attributes did not change`() = runUnitTest {
        given()
        val attributes = full()

        whenn()
        val diff = attributes.createDiffModel(full())

        then()
        assertNull(diff)
    }

    @Test
    fun `should send only the attribute that changed`() = runUnitTest {
        given()
        val older = full()

        whenn()
        val diff = full().copy(email = "new@example.com").createDiffModel(older)

        then()
        assertEquals(UserAttributesRemote(email = "new@example.com"), diff)
    }

    @Test
    fun `should send every attribute that changed`() = runUnitTest {
        given()
        val older = UserAttributesRemote()

        whenn()
        val diff = full().createDiffModel(older)

        then()
        assertEquals(full(), diff)
    }

    @Test
    fun `should keep attributes sent before for those not set now`() = runUnitTest {
        given()
        val older = full()

        whenn()
        val accumulated = UserAttributesRemote(firstName = "Olena").createAccModel(older)

        then()
        assertEquals(full().copy(firstName = "Olena"), accumulated)
    }

    @Test
    fun `should keep attributes set now over those sent before`() = runUnitTest {
        given()
        val older = UserAttributesRemote(phone = "+1", fields = listOf(UserCustomFieldRemote("plan", "pro")))

        whenn()
        val accumulated = full().createAccModel(older)

        then()
        assertEquals(full(), accumulated)
    }

    @Test
    fun `should merge custom fields sent before with those set now`() = runUnitTest {
        given()
        val older = UserAttributesRemote(phone = "+1", fields = listOf(UserCustomFieldRemote("age", "30")))

        whenn()
        val newer = UserAttributesRemote(fields = listOf(UserCustomFieldRemote("plan", "pro")))
        val accumulated = newer.createAccModel(older)

        then()
        assertEquals(
            listOf(UserCustomFieldRemote("age", "30"), UserCustomFieldRemote("plan", "pro")),
            accumulated.fields
        )
    }

    @Test
    fun `should keep custom fields absent when neither side has any`() = runUnitTest {
        given()
        val older = UserAttributesRemote(phone = "+1")

        whenn()
        val accumulated = UserAttributesRemote(firstName = "Ann").createAccModel(older)

        then()
        assertNull(accumulated.fields)
    }

    @Test
    fun `should send nothing after an accumulated user without custom fields is repeated`() = runUnitTest {
        given()
        val synced = UserAttributesRemote(firstName = "Ann").createAccModel(UserAttributesRemote(phone = "+1"))

        whenn()
        val diff = UserAttributesRemote(phone = "+1", firstName = "Ann").createDiffModel(synced)

        then()
        assertNull(diff)
    }

    @Test
    fun `should accumulate nothing new from equal or absent attributes`() = runUnitTest {
        given()
        val attributes = UserAttributesRemote(firstName = "Ann")

        whenn()
        val accumulated = listOf(
            attributes.createAccModel(null),
            attributes.createAccModel(UserAttributesRemote(firstName = "Ann"))
        )

        then()
        assertEquals(listOf(attributes, attributes), accumulated)
    }

    @Test
    fun `should keep custom fields of whichever side has them`() = runUnitTest {
        given()
        val older = UserAttributesRemote(phone = "+1", fields = listOf(UserCustomFieldRemote("age", "30")))
        val newer = UserAttributesRemote(firstName = "Ann", fields = listOf(UserCustomFieldRemote("plan", "pro")))

        whenn()
        val accumulated = listOf(
            UserAttributesRemote(firstName = "Ann").createAccModel(older).fields,
            newer.createAccModel(UserAttributesRemote(phone = "+1")).fields
        )

        then()
        assertEquals(
            listOf(listOf(UserCustomFieldRemote("age", "30")), listOf(UserCustomFieldRemote("plan", "pro"))),
            accumulated
        )
    }

    private fun full() = UserAttributesRemote(
        phone = "+380501234567",
        email = "ann@example.com",
        firstName = "Ann",
        lastName = "Lee",
        languageCode = "uk",
        timeZone = "Europe/Kyiv",
        marketId = "m-1",
        address = AddressRemote(region = "Kyiv", town = "Kyiv", address = "Main st 1", postcode = "01001"),
        fields = listOf(UserCustomFieldRemote("plan", "pro"))
    )
}
