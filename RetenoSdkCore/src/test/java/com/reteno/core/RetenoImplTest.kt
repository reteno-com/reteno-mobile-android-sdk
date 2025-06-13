package com.reteno.core

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.AppLifecycleController
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.IamController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.controller.ScreenTrackingController
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.features.appinbox.AppInboxImpl
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import com.reteno.core.view.iam.IamView
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
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

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            mockkConstructor(ServiceLocator::class)
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            unmockkConstructor(ServiceLocator::class)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var screenTrackingController: ScreenTrackingController

    @RelaxedMockK
    private lateinit var inbox: AppInboxImpl

    @RelaxedMockK
    private lateinit var iamView: IamView

    @RelaxedMockK
    private lateinit var iamController: IamController

    @RelaxedMockK
    private lateinit var activityHelper: RetenoActivityHelper

    @RelaxedMockK
    private lateinit var appLifeController: AppLifecycleController

    private var contextWrapper: ContextWrapper? = null

    private val transcript: MutableList<String> = mutableListOf()
    // endregion helper fields ---------------------------------------------------------------------

    @Before
    override fun before() {
        super.before()
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().iamViewProvider.get() } returns iamView
        every { anyConstructed<ServiceLocator>().eventsControllerProvider.get() } returns eventController
        every { anyConstructed<ServiceLocator>().iamControllerProvider.get() } returns iamController
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns activityHelper
        every { anyConstructed<ServiceLocator>().appInboxProvider.get() } returns inbox
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns screenTrackingController
        every { anyConstructed<ServiceLocator>().appLifecycleControllerProvider.get() } returns appLifeController

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
    fun givenExternalIdValid_whenSetUserAttributes_thenInteractWithController() = runRetenoTest { retenoImpl ->
        // When
        retenoImpl.setUserAttributes(externalUserId = EXTERNAL_USER_ID)
        advanceUntilIdle()
        // Then
        coVerify(exactly = 1) {
            contactController.setExternalIdAndUserData(
                eq(EXTERNAL_USER_ID),
                null
            )
        }
        //  verify(exactly = 1) { contactController.setUserData(null) }
    }

    @Test
    fun givenExternalIdNotNullAndUserNull_whenSetUserAttributes_thenInteractWithController() =
        runRetenoTest {retenoImpl ->
            // When
            retenoImpl.setUserAttributes(EXTERNAL_USER_ID, null)
            advanceUntilIdle()
            // Then
            coVerify(exactly = 1) {
                contactController.setExternalIdAndUserData(
                    eq(EXTERNAL_USER_ID),
                    null
                )
            }
        }

    @Test
    fun givenExternalIdAndUserProvided_whenSetUserAttributesWithUser_thenInteractWithController() =
        runRetenoTest {retenoImpl ->
            // Given
            val userFull = getUserFull()
            // When
            retenoImpl.setUserAttributes(EXTERNAL_USER_ID, userFull)
            advanceUntilIdle()
            // Then
            coVerify { contactController.setExternalIdAndUserData((eq(EXTERNAL_USER_ID)), userFull) }
            //  verify { contactController.setUserData(userFull) }
        }

    @Test
    fun givenExceptionThrown_whenSetUserData_thenExceptionSentToLogger() = runRetenoTest {retenoImpl ->
        // Given
        coEvery { contactController.setExternalIdAndUserData(any(), any()) } throws EXCEPTION

        val userFull = getUserFull()

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.setUserAttributes(EXTERNAL_USER_ID, userFull)
            false
        } catch (ex: Throwable) {
            true
        }
        advanceUntilIdle()
        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("setUserAttributes(): externalUserId = [$EXTERNAL_USER_ID], user = [$userFull]"),
                eq(EXCEPTION)
            )
        }
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
    fun givenAnonymousUserAttributesProvided_whenSetAnonymousUserAttributes_thenInteractWithController() =
        runRetenoTest {retenoImpl ->
            // Given
            val userAttributesAnonymous = getUserAttributesAnonymous()

            // When
            retenoImpl.setAnonymousUserAttributes(userAttributesAnonymous)

            // Then
            coVerify(exactly = 0) { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
            verify(exactly = 1) {
                contactController.setAnonymousUserAttributes(
                    eq(
                        userAttributesAnonymous
                    )
                )
            }
        }

    @Test
    fun givenExceptionThrown_whenSetAnonymousUserAttributes_thenExceptionSentToLogger() = runRetenoTest {retenoImpl ->
        // Given
        every { contactController.setAnonymousUserAttributes(any()) } throws EXCEPTION
        val userAttributesAnonymous = getUserAttributesAnonymous()

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.setAnonymousUserAttributes(userAttributesAnonymous)
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("setAnonymousUserAttributes(): userAttributes = [$userAttributesAnonymous]"),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenLogEvent_thenInteractWithEventController() = runRetenoTest {retenoImpl ->
        // Given
        val event = getCustomEvent()

        // When
        retenoImpl.logEvent(event)

        // Then
        verify(exactly = 1) { eventController.trackEvent(event) }
    }


    @Test
    fun givenExceptionThrown_whenLogEvent_thenExceptionSentToLogger() = runRetenoTest {retenoImpl ->
        // Given
        every { eventController.trackEvent(any()) } throws EXCEPTION
        val event = getCustomEvent()

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.logEvent(event)
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("logEvent(): event = [$event]"),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenLogScreenView_thenInteractWithEventController() = runRetenoTest { retenoImpl ->
        // When
        retenoImpl.logScreenView(TRACK_SCREEN_NAME)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TRACK_SCREEN_NAME) }
    }

    @Test
    fun givenExceptionThrown_whenLogScreenView_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { eventController.trackScreenViewEvent(any()) } throws EXCEPTION

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.logScreenView(TRACK_SCREEN_NAME)
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("logScreenView(): screenName = [$TRACK_SCREEN_NAME]"),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenLogEcomEvent_thenInteractWithEventController() = runRetenoTest { retenoImpl ->
        // Given
        val ecomEvent = getEcomEvent()

        // When
        retenoImpl.logEcommerceEvent(ecomEvent)

        // Then
        verify { eventController.trackEcomEvent(ecomEvent) }
    }

    @Test
    fun givenExceptionThrown_whenLogEcomEvent_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { eventController.trackEcomEvent(any()) } throws EXCEPTION
        val ecomEvent = getEcomEvent()

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.logEcommerceEvent(ecomEvent)
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("logEcommerceEvent(): ecomEvent = [$ecomEvent]"),
                eq(EXCEPTION)
            )
        }
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
        verify { scheduleController.startScheduler() }
        coVerify(exactly = 1) { contactController.checkIfDeviceRegistered() }
        coVerify(exactly = 1) { contactController.checkIfDeviceRequestSentThisSession() }
        verify { iamView.start() }
    }

    @Test
    fun givenExceptionThrown_whenResumeApp_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { scheduleController.startScheduler() } throws EXCEPTION

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
        verify { scheduleController.stopScheduler() }
        verify { iamView.pause() }
    }

    @Test
    fun givenExceptionThrown_whenPauseApp_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { scheduleController.stopScheduler() } throws EXCEPTION
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
        verify(exactly = 1) { scheduleController.forcePush() }
    }

    @Test
    fun givenExceptionThrown_whenForcePush_thenExceptionSentToLogger() = runRetenoTest { retenoImpl ->
        // Given
        every { scheduleController.forcePush() } throws EXCEPTION
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
        assertEquals(inbox, result)
    }

    @Test
    fun whenAppStart_thenBroadcastSent() = runRetenoTest { retenoImpl ->
        // Given
        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().iamViewProvider.get() } returns iamView
        every { anyConstructed<ServiceLocator>().eventsControllerProvider.get() } returns eventController
        every { anyConstructed<ServiceLocator>().appInboxProvider.get() } returns inbox
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns screenTrackingController
        mockQueryBroadcastReceivers(application)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    transcript.add(TRANSCRIPT_RESUME_RECEIVED)
                }
            }
        contextWrapper!!.registerReceiver(
            receiver,
            IntentFilter(Constants.BROADCAST_ACTION_RETENO_APP_RESUME)
        )

        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)
        val retenoImpl = createRetenoAndAdvanceInit(lifecycleOwner)
        // When

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // Then
        ShadowLooper.shadowMainLooper().idle()
        assertTrue(transcript.contains(TRANSCRIPT_RESUME_RECEIVED))
        RetenoInternalImpl.swapInstance(null)
    }

    @Test
    fun whenRetenoStart_thenGetInAppNotifications() = runTest {
        //Given
        val application = mockk<Application>()
        mockQueryBroadcastReceivers(application)
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        coEvery { contactController.checkIfDeviceRegistered() } returns Unit
        every { scheduleController.clearOldData() } returns Unit
        every { application.sendBroadcast(any()) } returns Unit
        //When
        val reteno = createRetenoAndAdvanceInit()

        reteno.start()

        //Then
        verify(exactly = 1) { iamController.getInAppMessages() }
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