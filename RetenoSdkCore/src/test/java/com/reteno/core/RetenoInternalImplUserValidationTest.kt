package com.reteno.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.di.ServiceLocator
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.coroutine.retenoDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoInternalImplUserValidationTest {

    private class SutFixture(scope: TestScope) {
        private val application = ApplicationProvider.getApplicationContext<Application>()
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val actions = FakeSdkActions()
        val sut = RetenoInternalImpl(
            application = application,
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
            appLifecycleOwner = ManualLifecycleOwner(),
            serviceLocator = ServiceLocator(application, retenoDispatchers(dispatcher, dispatcher)),
            sdkActions = { actions },
            isOsSupported = { true }
        )
    }

    @Test
    fun `should throw to caller for blank external user id before sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = runCatching { fixture.sut.setUserAttributes("  ", stubUser()) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(false, fixture.sut.isInitialized)
        assertEquals(listOf("boot"), fixture.actions.calls)
    }

    @Test
    fun `should throw to caller for blank external user id without user`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = runCatching { fixture.sut.setUserAttributes("") }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should throw to caller for invalid market id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = runCatching { fixture.sut.setUserAttributes("user-1", stubUser(marketId = "bad market!")) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should throw to caller for blank multi account user id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = runCatching { fixture.sut.setMultiAccountUserAttributes(" ", null) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should throw to caller for invalid anonymous market id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = runCatching { fixture.sut.setAnonymousUserAttributes(stubAnonymousAttributes(marketId = "x".repeat(65))) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should accept valid user attributes before sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = runCatching {
            fixture.sut.setUserAttributes("user-1", stubUser())
            fixture.sut.setMultiAccountUserAttributes("user-1", null)
            fixture.sut.setAnonymousUserAttributes(stubAnonymousAttributes())
        }

        then()
        assertTrue(result.isSuccess)
    }
}
