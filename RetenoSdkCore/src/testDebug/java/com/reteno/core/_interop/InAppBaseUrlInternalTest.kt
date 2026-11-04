package com.reteno.core._interop

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class InAppBaseUrlInternalTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val raw = application.getSharedPreferences("reteno_shared_prefs", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
    }

    @Test
    fun `should read back the in-app base url override it stored`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        InAppBaseUrlInternal.setBaseUrl(fixture.application, BASE_URL)

        then()
        assertEquals(BASE_URL, InAppBaseUrlInternal.baseUrl(fixture.application))
    }

    @Test
    fun `should remove the in-app base url override when cleared`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit().putString("iam_base_url", BASE_URL).commit()

        whenn()
        InAppBaseUrlInternal.setBaseUrl(fixture.application, null)

        then()
        assertNull(InAppBaseUrlInternal.baseUrl(fixture.application))
    }

    companion object {
        private const val BASE_URL = "https://cdn.example/base.html"
    }
}
