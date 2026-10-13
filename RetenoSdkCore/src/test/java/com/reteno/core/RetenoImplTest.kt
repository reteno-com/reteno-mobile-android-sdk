package com.reteno.core

import android.app.Application
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import java.time.ZonedDateTime


@OptIn(ExperimentalCoroutinesApi::class)
class RetenoImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val EXTERNAL_USER_ID = "external_user_ID"
        private val USER_SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")

        private val USER_GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val USER_GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")
        private const val EVENT_TYPE_KEY = "EVENT_TYPE_KEY"

        private const val EVENT_PARAMETER_KEY_1 = "KEY1"
        private const val EVENT_PARAMETER_VALUE_1 = "VALUE1"

        private const val TRACK_SCREEN_NAME = "ScreenNameHere"

        private const val ECOM_EVENT_EXTERNAL_ORDER_ID = "external_order_id"

        private const val TRANSCRIPT_RESUME_RECEIVED = "ResumeReceived"

        private const val FIRST_NAME = "firstName1"
        private const val LAST_NAME = "lastName1"
        private const val LANGUAGE_CODE = "languageCode1"
        private const val TIME_ZONE = "timeZone1"
        private const val MARKET_ID = "marketId"
        private const val FIELD_KEY1 = "key1"
        private const val FIELD_KEY2 = "key2"
        private const val FIELD_KEY3 = "key3"
        private const val FIELD_VALUE1 = "value1"
        private const val FIELD_VALUE2 = "value2"

        private const val REGION = "region1"
        private const val TOWN = "town1"
        private const val ADDRESS = "address1"
        private const val POSTCODE = "postcode1"

        private val addressFull = Address(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        private val customFieldsFull = listOf(
            UserCustomField(FIELD_KEY1, FIELD_VALUE1),
            UserCustomField(FIELD_KEY2, FIELD_VALUE2),
            UserCustomField(FIELD_KEY3, null)
        )

        private val EXCEPTION = Exception("MyCustomException")

    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------

    private var contextWrapper: ContextWrapper? = null

    private val transcript: MutableList<String> = mutableListOf()
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        contextWrapper = ContextWrapper(application)
        assertNotNull(contextWrapper)
        transcript.clear()
    }

    override fun after() {
        super.after()
        contextWrapper = null
        transcript.clear()
    }

    @Test
    fun givenExternalIdBlank_whenSetUserAttributesWithUser_thenThrowException() = runRetenoTest {retenoImpl ->
        // Given
        val expectedException =
            IllegalArgumentException("externalUserId should not be null or blank")
        // When
        val actualException = try {
            retenoImpl.setUserAttributes(" ")
            null
        } catch (e: Exception) {
            e
        }

        // Then
        assertTrue(actualException is java.lang.IllegalArgumentException)
        assertEquals(expectedException.message, actualException?.message)
    }


    @Test
    fun whenAutoScreenTracking_thenInteractWithActivityHelper() = runRetenoTest { retenoImpl ->
        // Given
        val config = getScreenTrackingConfig()

        // When
        retenoImpl.autoScreenTracking(config)

        // Then
        verify(exactly = 1) { screenTrackingController.autoScreenTracking(config) }
    }

    @Test
    fun givenExceptionThrown_whenAutoScreenTracking_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { screenTrackingController.autoScreenTracking(any()) } throws EXCEPTION
        val config = getScreenTrackingConfig()

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.autoScreenTracking(config)
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("autoScreenTracking(): config = [$config]"),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenResumeApp_thenStartScheduler() = runRetenoTest { retenoImpl ->
        //Given

        // When
        retenoImpl.start()

        advanceUntilIdle()

        // Then
        verify { dataSync.startScheduler() }
        verify { iamView.start() }
    }

    @Test
    fun givenExceptionThrown_whenResumeApp_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { dataSync.startScheduler() } throws EXCEPTION

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.start()
            advanceUntilIdle()
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("start(): "),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenPauseApp_thenStopScheduler() = runRetenoTest { retenoImpl ->
        //Given
        // When
        retenoImpl.stop()

        // Then
        verify { dataSync.stopScheduler() }
        verify { iamView.pause() }
    }

    @Test
    fun givenExceptionThrown_whenPauseApp_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { dataSync.stopScheduler() } throws EXCEPTION
        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.stop()
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("stop(): "),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenForcePush_thenCallScheduleController() = runRetenoTest { retenoImpl ->
        //Given
        // When
        retenoImpl.forcePushData()

        // Then
        verify(exactly = 1) { dataSync.request() }
    }

    @Test
    fun givenExceptionThrown_whenForcePush_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { dataSync.request() } throws EXCEPTION
        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.forcePushData()
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("forcePushData(): "),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun getAppInbox() = runRetenoTest { retenoImpl ->
        val result = retenoImpl.appInbox
        assertEquals(appInbox, result)
    }

    @Test
    fun whenAppStart_thenBroadcastSent() {
        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)
        runRetenoTest(lifecycleOwner) { retenoImpl ->
            // Given

            // When
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

            // Then
            ShadowLooper.shadowMainLooper().idle()
            verify { appLifecycleController.start() }
        }
    }

    @Test
    fun whenRetenoStart_thenGetInAppNotifications() = runTest {
        //Given
        val application = mockk<Application>()
        mockQueryBroadcastReceivers(application)
        every { application.sendBroadcast(any()) } returns Unit
        //When
        val reteno = createRetenoAndAdvanceInit()

        reteno.start()
        advanceUntilIdle()
        //Then
        coVerify(exactly = 1) { iamController.getInAppMessages() }
        RetenoInternalImpl.swapInstance(null)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockQueryBroadcastReceivers(application: Application) {
        val mockResolveInfo = mockk<ResolveInfo>(relaxed = true).apply {
            activityInfo = ActivityInfo().apply {
                packageName = "packageName"
                name = "name"
            }
        }
        every { application.queryBroadcastReceivers(any()) } returns listOf(mockResolveInfo)
    }

    private fun getUserFull() = User(
        userAttributes = null,
        subscriptionKeys = USER_SUBSCRIPTION_KEYS,
        groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
        groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
    )

    private fun getUserAttributesAnonymous() = UserAttributesAnonymous(
        firstName = FIRST_NAME,
        lastName = LAST_NAME,
        languageCode = LANGUAGE_CODE,
        timeZone = TIME_ZONE,
        marketId = MARKET_ID,
        address = addressFull,
        fields = customFieldsFull
    )

    private fun getCustomEvent() = Event.Custom(
        typeKey = EVENT_TYPE_KEY,
        dateOccurred = ZonedDateTime.now(),
        parameters = listOf(Parameter(EVENT_PARAMETER_KEY_1, EVENT_PARAMETER_VALUE_1))
    )

    private fun getEcomEvent() = EcomEvent.OrderCancelled(
        ECOM_EVENT_EXTERNAL_ORDER_ID,
        ZonedDateTime.now()
    )

    private fun getScreenTrackingConfig() =
        ScreenTrackingConfig(true, listOf(), ScreenTrackingTrigger.ON_RESUME)
    // endregion helper methods --------------------------------------------------------------------
}