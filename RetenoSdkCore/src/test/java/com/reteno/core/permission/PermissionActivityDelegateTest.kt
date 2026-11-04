package com.reteno.core.permission

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.installCapturing
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class PermissionActivityDelegateTest {

    private class SutFixture(scope: TestScope) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val dispatchers = scope.testDispatchers()
        val sut = PermissionActivityDelegate(application, dispatchers.main)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity: ComponentActivity = controller.get()
    }

    class ThreadProbeActivity : ComponentActivity() {
        var probe: () -> Boolean = { false }
        val readsOnMain = mutableListOf<Boolean>()

        override fun isFinishing(): Boolean {
            readsOnMain += probe()
            return super.isFinishing()
        }
    }

    @Test
    fun `should hand out the permission checker of the resumed activity`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.controller.setup()

        whenn()
        val checker = fixture.sut.requestChecker()

        then()
        assertSame(fixture.activity, checker?.awaitActivity())
    }

    @Test
    fun `should look for the resumed activity on the main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val probing = Robolectric.buildActivity(ThreadProbeActivity::class.java).setup()
        probing.get().probe = { fixture.dispatchers.markedMain.isRunningHere }
        probing.get().readsOnMain.clear()

        whenn()
        val checker = withContext(Dispatchers.Unconfined) { fixture.sut.requestChecker() }

        then()
        assertNotNull(checker)
        assertEquals(listOf(true), probing.get().readsOnMain)
    }

    @Test
    fun `should wait for the started activity to resume`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.controller.create().start()
        val checker = async { fixture.sut.requestChecker() }
        advanceTimeBy(500)

        whenn()
        fixture.controller.resume()
        advanceTimeBy(100)

        then()
        assertNotNull(checker.await())
    }

    @Test
    fun `should give no checker when no activity resumes within two seconds`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.controller.create().start()

        whenn()
        val checker = fixture.sut.requestChecker()

        then()
        assertNull(checker)
    }

    @Test
    fun `should give no checker for a finishing activity`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.controller.setup()
        fixture.activity.finish()

        whenn()
        val checker = fixture.sut.requestChecker()

        then()
        assertNull(checker)
    }

    @Test
    fun `should give no checker for an activity that cannot ask for permissions`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        Robolectric.buildActivity(Activity::class.java).setup()

        whenn()
        val checker = fixture.sut.requestChecker()

        then()
        assertNull(checker)
    }

    @Test
    fun `should forget the activity once it stops`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val other = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        fixture.controller.setup()
        other.pause().stop()

        whenn()
        val whileOtherStopped = fixture.sut.requestChecker()
        fixture.controller.pause().stop()
        val afterStop = fixture.sut.requestChecker()

        then()
        assertNotNull(whileOtherStopped)
        assertNull(afterStop)
    }

    @Test
    fun `should drop the checker of a destroyed activity`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.controller.setup()
        val checker = fixture.sut.requestChecker()

        whenn()
        fixture.controller.pause().stop().destroy()
        fixture.sut.onActivityStarted(fixture.activity)

        then()
        assertNotNull(checker)
        assertNull(fixture.sut.requestChecker())
    }

    @Test
    fun `should not hang a request whose activity is recreated before the answer`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.controller.setup()
        val checker = requireNotNull(fixture.sut.requestChecker())
        val request = async { checker.check(Manifest.permission.CAMERA) }
        runCurrent()
        val requestCode = shadowOf(fixture.activity).lastRequestedPermission.requestCode

        whenn()
        fixture.controller.recreate()
        val recreated = fixture.controller.get()
        recreated.onRequestPermissionsResult(
            requestCode,
            arrayOf(Manifest.permission.CAMERA),
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )
        runCurrent()

        then()
        assertFalse(request.await())
    }

    @Test
    fun `should report an activity it learns about too late to ask for permissions`() = runUnitTest {
        given()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val late = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val delegate = PermissionActivityDelegate(application, testDispatchers().main)
        val remoteLog = FakeRemoteLog()

        whenn()
        Logger.installCapturing(FakeLogSink(), remoteLog)
        try {
            delegate.onActivityCreated(late, null)
        } finally {
            Logger.install(AndroidLogSink(), null)
        }
        delegate.onActivityStarted(late)

        then()
        assertEquals(1, remoteLog.errors.size)
        assertNull(delegate.requestChecker())
    }
}
