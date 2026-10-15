package com.reteno.core.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class SdkComponentTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val sut = SdkComponent(
            context = application,
            dispatchers = object : RetenoDispatchers {
                override val main = Dispatchers.Unconfined
                override val io = Dispatchers.Unconfined
                override val default = Dispatchers.Unconfined
            }
        )

        fun collaborators(): List<Any> = listOf(
            sut.prefs,
            sut.restConfig,
            sut.restClient,
            sut.configRepository,
            sut.fileManager,
            sut.activityHelper,
            sut.databaseManager,
            sut.devicesDb,
            sut.usersDb,
            sut.interactionsDb,
            sut.inAppInteractionsDb,
            sut.eventsDb,
            sut.inboxDb,
            sut.inAppMessagesDb,
            sut.recomEventsDb,
            sut.wrappedLinksDb,
            sut.logEventsDb
        )
    }

    @Test
    fun `should hand out one instance of each shared collaborator`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val first = fixture.collaborators()

        whenn()
        System.gc()
        val second = fixture.collaborators()

        then()
        first.zip(second).forEach { (before, after) -> assertSame(before, after) }
    }
}
