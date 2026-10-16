package com.reteno.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.fixtures.FakeFacadeInfrastructure
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubUser
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private class SutFixture {
        private val application = ApplicationProvider.getApplicationContext<Application>()
        val actions = FakeSdkActions()
        val infrastructure = FakeFacadeInfrastructure(actions)
        val sut = RetenoInternalImpl(
            application = application,
            appLifecycleOwner = ManualLifecycleOwner(),
            infrastructure = infrastructure
        )
    }

    @Test
    fun `should throw to caller for blank external user id before sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()

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
        val fixture = SutFixture()

        whenn()
        val result = runCatching { fixture.sut.setUserAttributes("") }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should throw to caller for invalid market id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val result = runCatching { fixture.sut.setUserAttributes("user-1", stubUser(marketId = "bad market!")) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should throw to caller for blank multi account user id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val result = runCatching { fixture.sut.setMultiAccountUserAttributes(" ", null) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should throw to caller for invalid anonymous market id`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val attributes = stubAnonymousAttributes(marketId = "x".repeat(65))

        whenn()
        val result = runCatching { fixture.sut.setAnonymousUserAttributes(attributes) }

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should not forward user attributes with invalid market id once sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.actions.markInitialized()

        whenn()
        runCatching { fixture.sut.setUserAttributes("user-1", stubUser(marketId = "bad market!")) }

        then()
        assertEquals(listOf("boot"), fixture.actions.calls)
    }

    @Test
    fun `should not forward multi account user attributes with blank user id once sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.actions.markInitialized()

        whenn()
        runCatching { fixture.sut.setMultiAccountUserAttributes(" ", stubUser()) }

        then()
        assertEquals(listOf("boot"), fixture.actions.calls)
    }

    @Test
    fun `should not forward anonymous user attributes with invalid market id once sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.actions.markInitialized()
        val attributes = stubAnonymousAttributes(marketId = "x".repeat(65))

        whenn()
        runCatching { fixture.sut.setAnonymousUserAttributes(attributes) }

        then()
        assertEquals(listOf("boot"), fixture.actions.calls)
    }

    @Test
    fun `should accept valid user attributes before sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()

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
