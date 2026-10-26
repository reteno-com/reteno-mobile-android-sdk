package com.reteno.core.permission

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidPermissionCheckerTest {

    private class SutFixture {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity: ComponentActivity = controller.get()
        val sut = AndroidPermissionChecker()

        fun attach() = apply {
            controller.create()
            sut.attachActivity(activity)
            controller.start().resume()
        }

        fun answerLastRequest(vararg granted: Boolean) {
            val request = shadowOf(activity).lastRequestedPermission
            val results = granted.map { if (it) GRANTED else DENIED }
            activity.onRequestPermissionsResult(
                request.requestCode,
                request.requestedPermissions,
                results.toIntArray()
            )
        }

        fun requestedPermissions(): List<String> =
            shadowOf(activity).lastRequestedPermission.requestedPermissions.toList()
    }

    @Test
    fun `should grant a permission the app already has without asking`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(CAMERA)

        whenn()
        val granted = async { fixture.sut.check(CAMERA) }
        runCurrent()
        ShadowLooper.idleMainLooper()

        then()
        assertTrue(granted.await())
    }

    @Test
    fun `should ask the user for a permission and pass on the answer`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()

        whenn()
        val answers = listOf(true, false).map { answer ->
            val request = async { fixture.sut.check(CAMERA) }
            runCurrent()
            fixture.answerLastRequest(answer)
            request.await()
        }

        then()
        assertEquals(listOf(true, false), answers)
        assertEquals(listOf(CAMERA), fixture.requestedPermissions())
    }

    @Test
    fun `should ask for several permissions at once`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()

        whenn()
        val request = async { fixture.sut.check(CAMERA, LOCATION) }
        runCurrent()
        fixture.answerLastRequest(true, false)

        then()
        assertEquals(mapOf(CAMERA to true, LOCATION to false), request.await())
    }

    @Test
    fun `should ask one permission at a time`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()
        val first = async { fixture.sut.check(CAMERA) }
        val second = async { fixture.sut.check(LOCATION) }
        runCurrent()

        whenn()
        val askedFirst = fixture.requestedPermissions()
        fixture.answerLastRequest(true)
        runCurrent()
        val askedSecond = fixture.requestedPermissions()
        fixture.answerLastRequest(false)

        then()
        assertEquals(listOf(listOf(CAMERA), listOf(LOCATION)), listOf(askedFirst, askedSecond))
        assertEquals(listOf(true, false), listOf(first.await(), second.await()))
    }

    @Test
    fun `should ignore an answer nobody waits for`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()
        val request = async { fixture.sut.check(CAMERA) }
        runCurrent()
        fixture.answerLastRequest(true)
        request.await()

        whenn()
        fixture.answerLastRequest(false)

        then()
        assertTrue(request.await())
    }

    @Test
    fun `should not grant anything once its activity is gone`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()
        fixture.controller.pause().stop().destroy()

        whenn()
        val single = fixture.sut.check(CAMERA)
        val several = fixture.sut.check(CAMERA, LOCATION)

        then()
        assertFalse(single)
        assertEquals(mapOf(CAMERA to false, LOCATION to false), several)
    }

    @Test
    fun `should not grant anything before an activity is attached`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val granted = fixture.sut.check(CAMERA)

        then()
        assertFalse(granted)
    }

    @Test
    fun `should answer a pending request as not granted when its activity is destroyed`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()
        val request = async { fixture.sut.check(CAMERA) }
        runCurrent()

        whenn()
        fixture.controller.pause().stop().destroy()
        runCurrent()

        then()
        assertFalse(request.await())
    }

    @Test
    fun `should answer a pending multi-permission request as not granted when its activity is destroyed`() =
        runUnitTest {
            given()
            val fixture = SutFixture().attach()
            val request = async { fixture.sut.check(CAMERA, LOCATION) }
            runCurrent()

            whenn()
            fixture.controller.pause().stop().destroy()
            runCurrent()

            then()
            assertEquals(mapOf(CAMERA to false, LOCATION to false), request.await())
        }

    @Test
    fun `should do nothing when a checker that was never attached is destroyed`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.onDestroy(fixture.activity)

        then()
        assertFalse(fixture.sut.check(CAMERA))
    }

    @Test
    fun `should give the attached activity as context`() = runUnitTest {
        given()
        val fixture = SutFixture().attach()

        whenn()
        val context = fixture.sut.awaitContext()

        then()
        assertSame(fixture.activity, context)
    }

    @Test
    fun `should wait for an activity to be attached`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val awaited = async { fixture.sut.awaitActivity() }
        advanceTimeBy(500)

        whenn()
        fixture.attach()
        advanceTimeBy(50)

        then()
        assertSame(fixture.activity, awaited.await())
    }

    @Test
    fun `should stop waiting for an activity after two seconds`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val failure = runCatching { fixture.sut.awaitActivity() }.exceptionOrNull()

        then()
        assertTrue(failure is TimeoutCancellationException)
        assertEquals(2_000L, currentTime)
    }

    companion object {
        private const val CAMERA = Manifest.permission.CAMERA
        private const val LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val GRANTED = PackageManager.PERMISSION_GRANTED
        private const val DENIED = PackageManager.PERMISSION_DENIED
    }
}
