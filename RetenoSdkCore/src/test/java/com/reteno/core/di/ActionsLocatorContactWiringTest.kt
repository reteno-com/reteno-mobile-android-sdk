package com.reteno.core.di

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.model.iam.displayrules.async.AsyncDisplayRules
import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubSegment
import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.action.sync.SyncResult
import com.reteno.core.internal.log.LogPriority
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionsLocatorContactWiringTest {

    @Test
    fun `should register device in local database and sync contact`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        coEvery { fixture.configRepository.getFcmToken() } returns "token"

        whenn()
        val result = fixture.sut.registerDevice().getOrThrow()

        then()
        assertEquals(SyncResult.COMPLETED, result)
        verify(exactly = 1) { fixture.devicesDb.insertDevice(any()) }
    }

    @Test
    fun `should ensure device registration once per session`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        coEvery { fixture.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.sut.ensureDeviceRegisteredThisSession()
        fixture.sut.ensureDeviceRegisteredThisSession()

        then()
        verify(exactly = 1) { fixture.devicesDb.insertDevice(any()) }
    }

    @Test
    fun `should store new push token`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        every { fixture.configRepository.getStoredFcmToken() } returns "old"

        whenn()
        fixture.sut.updateFcmToken("new")

        then()
        verify(exactly = 1) { fixture.configRepository.saveFcmToken("new") }
    }

    @Test
    fun `should store changed notification state`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        every { fixture.configRepository.isNotificationsEnabled() } returns true

        whenn()
        fixture.sut.updateNotificationsEnabled(false)

        then()
        verify(exactly = 1) { fixture.configRepository.saveNotificationsEnabled(false) }
    }

    @Test
    fun `should save user and refresh in-app segmentation after debounce`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        fixture.sut.requestStart()
        runCurrent()
        fixture.sut.inAppMessagesState.replaceMessages(
            listOf(stubInAppMessage(async = AsyncDisplayRules(segment = stubSegment(inSegment = false))))
        )

        whenn()
        fixture.sut.setUserAttributes("user-1", stubUser())
        advanceTimeBy(5_001)
        runCurrent()

        then()
        verify(exactly = 1) { fixture.usersDb.insertUser(any()) }
        assertEquals(1, fixture.rest.requests.count { it.contract == ApiContract.InAppMessages.CheckUserInSegments })
    }

    @Test
    fun `should switch account through sdk lifecycle and session handler`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.setMultiAccountUserAttributes("account-1", null)
        runCurrent()

        then()
        verifyOrder {
            fixture.sessionHandler.stop()
            fixture.sessionHandler.clearSessionForced()
            fixture.sessionHandler.start()
        }
        verify(exactly = 1) { fixture.configRepository.setDeviceIdSuffix("account-1") }
    }

    @Test
    fun `should save anonymous attributes`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.setAnonymousUserAttributes(stubAnonymousAttributes())

        then()
        verify(exactly = 1) { fixture.usersDb.insertUser(any()) }
    }

    @Test
    fun `should report rejected anonymous attributes at info level`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.setAnonymousUserAttributes(UserAttributesAnonymous())
        runCurrent()

        then()
        val expected = listOf("SetAnonymousUserAttributes: attributes rejected by validation" to LogLevel.INFO)
        assertEquals(expected, fixture.remoteLogs)
    }

    @Test
    fun `should log failed contact task and keep queue running`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        var nextRan = false

        whenn()
        fixture.sut.contactQueue.enqueue { throw IllegalStateException("boom") }
        fixture.sut.contactQueue.enqueue { nextRan = true }
        runCurrent()

        then()
        assertTrue(nextRan)
        val entry = fixture.sink.entries.single { it.message.startsWith("Contact task failed") }
        assertEquals(LogPriority.ERROR, entry.priority)
    }
}
