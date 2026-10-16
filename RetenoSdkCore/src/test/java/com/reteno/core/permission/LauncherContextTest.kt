package com.reteno.core.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class LauncherContextTest {

    private class SutFixture(contract: ActivityResultContract<String, Boolean> = RequestPermission()) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()
        val unansweredInputs = mutableListOf<String>()
        val sut = LauncherContext(controller.get(), contract) {
            unansweredInputs += it
            false
        }

        init {
            controller.start().resume()
        }
    }

    private class FailingLaunch : ActivityResultContract<String, Boolean>() {

        override fun createIntent(context: Context, input: String): Intent = throw IllegalStateException(input)

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean = false
    }

    @Test
    fun `should forget a request cancelled before its answer`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val request = async { fixture.sut.launch(CAMERA) }
        runCurrent()

        whenn()
        request.cancel()
        runCurrent()
        fixture.sut.abandon()

        then()
        assertTrue(request.isCancelled)
        assertEquals(emptyList<String>(), fixture.unansweredInputs)
    }

    @Test
    fun `should forget a request whose launch failed`() = runUnitTest {
        given()
        val fixture = SutFixture(FailingLaunch())

        whenn()
        val failure = runCatching { fixture.sut.launch(CAMERA) }.exceptionOrNull()
        fixture.sut.abandon()

        then()
        assertTrue(failure is IllegalStateException)
        assertEquals(emptyList<String>(), fixture.unansweredInputs)
    }

    companion object {
        private const val CAMERA = Manifest.permission.CAMERA
    }
}
