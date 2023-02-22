package com.reteno.core

import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import com.reteno.core.appinbox.AppInboxImpl
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.ecom.EcomEvent
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.queryBroadcastReceivers
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.robolectric.shadows.ShadowLooper.shadowMainLooper
import java.lang.Exception
import java.time.ZonedDateTime


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
    private lateinit var retenoActivityHelper: RetenoActivityHelper

    @RelaxedMockK
    private lateinit var inbox: AppInboxImpl

    private val retenoImpl by lazy { RetenoImpl(application, "") }

    private var contextWrapper: ContextWrapper? = null

    private val transcript: MutableList<String> = mutableListOf()
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().eventsControllerProvider.get() } returns eventController
        every { anyConstructed<ServiceLocator>().appInboxProvider.get() } returns inbox
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns retenoActivityHelper

        contextWrapper = ContextWrapper(application)
        assertNotNull(contextWrapper)
        transcript.clear()
    }

    override fun after() {
        super.after()
        unmockkConstructor(ServiceLocator::class)
        contextWrapper = null
        transcript.clear()
    }

    @Test
    fun givenExternalIdValid_whenSetUserAttributes_thenInteractWithController() {
        // When
        retenoImpl.setUserAttributes(externalUserId = EXTERNAL_USER_ID)

        // Then
        verify(exactly = 1) { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 1) { contactController.setUserData(null) }
    }

    @Test
    fun givenExternalIdNotNullAndUserNull_whenSetUserAttributes_thenInteractWithController() {
        // When
        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, null)

        // Then
        verify(exactly = 1) { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 1) { contactController.setUserData(null) }
    }

    @Test
    fun givenExternalIdAndUserProvided_whenSetUserAttributesWithUser_thenInteractWithController() {
        // Given
        val userFull = getUserFull()

        // When
        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, userFull)

        // Then
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify { contactController.setUserData(userFull) }
    }

    @Test
    fun givenExceptionThrown_whenSetUserData_thenExceptionSentToLogger() {
        // Given
        every { contactController.setUserData(any()) } throws EXCEPTION

        val userFull = getUserFull()

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.setUserAttributes(EXTERNAL_USER_ID, userFull)
            false
        } catch (ex: Throwable) {
            true
        }

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
    fun givenExternalIdBlank_whenSetUserAttributesWithUser_thenThrowException() {
        // Given
        val expectedException = IllegalArgumentException("externalUserId should not be null or blank")

        // When
        val actualException = try {
            retenoImpl.setUserAttributes(" ")
            null
        } catch (e: java.lang.Exception) {
            e
        }

        // Then
        assertTrue(actualException is java.lang.IllegalArgumentException)
        assertEquals(expectedException.message, actualException?.message)
    }

    @Test
    fun givenAnonymousUserAttributesProvided_whenSetAnonymousUserAttributes_thenInteractWithController() {
        // Given
        val userAttributesAnonymous = getUserAttributesAnonymous()

        // When
        retenoImpl.setAnonymousUserAttributes(userAttributesAnonymous)

        // Then
        verify(exactly = 0) { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 1) {
            contactController.setAnonymousUserAttributes(
                eq(
                    userAttributesAnonymous
                )
            )
        }
    }

    @Test
    fun givenExceptionThrown_whenSetAnonymousUserAttributes_thenExceptionSentToLogger() {
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
    fun whenLogEvent_thenInteractWithEventController() {
        // Given
        val event = getCustomEvent()

        // When
        retenoImpl.logEvent(event)

        // Then
        verify(exactly = 1) { eventController.trackEvent(event) }
    }



    @Test
    fun givenExceptionThrown_whenLogEvent_thenExceptionSentToLogger() {
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
    fun whenLogScreenView_thenInteractWithEventController() {
        // When
        retenoImpl.logScreenView(TRACK_SCREEN_NAME)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TRACK_SCREEN_NAME) }
    }

    @Test
    fun givenExceptionThrown_whenLogScreenView_thenExceptionSentToLogger() {
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
    fun whenLogEcomEvent_thenInteractWithEventController() {
        // Given
        val ecomEvent = getEcomEvent()

        // When
        retenoImpl.logEcommerceEvent(ecomEvent)

        // Then
        verify { eventController.trackEcomEvent(ecomEvent) }
    }

    @Test
    fun givenExceptionThrown_whenLogEcomEvent_thenExceptionSentToLogger() {
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
    fun whenAutoScreenTracking_thenInteractWithActivityHelper() {
        // Given
        val config = getScreenTrackingConfig()

        // When
        retenoImpl.autoScreenTracking(config)

        // Then
        verify(exactly = 1) { retenoActivityHelper.autoScreenTracking(config) }
    }

    @Test
    fun givenExceptionThrown_whenAutoScreenTracking_thenExceptionSentToLogger() {
        // Given
        every { retenoActivityHelper.autoScreenTracking(any()) } throws EXCEPTION
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
    fun whenResumeApp_thenStartScheduler() {
        // When
        retenoImpl.resume(mockk())

        // Then
        verify(exactly = 1) { scheduleController.startScheduler() }
        verify(exactly = 1) { contactController.checkIfDeviceRegistered() }
    }

    @Test
    fun whenResumeApp_thenCalledClearOleEvents() {
        // When
        retenoImpl.resume(mockk())

        // Then
        verify(exactly = 1) { scheduleController.clearOldData() }
    }

    @Test
    fun givenExceptionThrown_whenResumeApp_thenExceptionSentToLogger() {
        // Given
        every { scheduleController.startScheduler() } throws EXCEPTION

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.resume(mockk())
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("resume(): "),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenPauseApp_thenStopScheduler() {
        // When
        retenoImpl.pause(mockk())

        // Then
        verify { scheduleController.stopScheduler() }
    }

    @Test
    fun givenExceptionThrown_whenPauseApp_thenExceptionSentToLogger() {
        // Given
        every { scheduleController.stopScheduler() } throws EXCEPTION

        // When
        val exceptionThrownOutsideSdk = try {
            retenoImpl.pause(mockk())
            false
        } catch (ex: Throwable) {
            true
        }

        // Then
        assertFalse(exceptionThrownOutsideSdk)
        verify(exactly = 1) {
            Logger.e(
                any(),
                eq("pause(): "),
                eq(EXCEPTION)
            )
        }
    }

    @Test
    fun whenForcePush_thenCallScheduleController() {
        // When
        retenoImpl.forcePushData()

        // Then
        verify(exactly = 1) { scheduleController.forcePush() }
    }

    @Test
    fun givenExceptionThrown_whenForcePush_thenExceptionSentToLogger() {
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
    fun getAppInbox() {
        val retenoImpl = RetenoImpl(mockk(), "")

        val result = retenoImpl.appInbox
        assertEquals(inbox, result)
    }

    @Test
    fun whenAppResume_thenBroadcastSent() {
        // Given
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
        mockQueryBroadcastReceivers()

        // When
        retenoImpl.resume(mockk())

        // Then
        shadowMainLooper().idle()
        assertTrue(transcript.contains(TRANSCRIPT_RESUME_RECEIVED))
    }

    // region helper methods -----------------------------------------------------------------------
    private fun mockQueryBroadcastReceivers() {
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