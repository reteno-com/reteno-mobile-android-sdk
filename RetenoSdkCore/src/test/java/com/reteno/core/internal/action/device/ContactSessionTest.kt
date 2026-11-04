package com.reteno.core.internal.action.device

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ContactSessionTest {

    @Test
    fun `should start with device not sent`() = runUnitTest {
        given()
        val sut = ContactSession()

        whenn()
        val sent = sut.isDeviceSent

        then()
        assertFalse(sent)
    }

    @Test
    fun `should remember device sent until reset`() = runUnitTest {
        given()
        val sut = ContactSession()

        whenn()
        sut.markDeviceSent()
        val afterSent = sut.isDeviceSent
        sut.markDeviceNotSent()

        then()
        assertTrue(afterSent)
        assertFalse(sut.isDeviceSent)
    }
}
