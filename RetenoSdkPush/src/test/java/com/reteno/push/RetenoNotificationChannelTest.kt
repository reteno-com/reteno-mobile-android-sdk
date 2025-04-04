package com.reteno.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.reteno.core.Reteno
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Util
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import io.mockk.*
import junit.framework.TestCase.*
import org.junit.*
import org.powermock.reflect.Whitebox
import org.robolectric.annotation.Config


@Config(sdk = [26])
class RetenoNotificationChannelTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEFAULT_CHANNEL_ID = "CHANNEL_ID"
        private val FALLBACK_DEFAULT_CHANNEL_NAME = Whitebox.getField(
            RetenoNotificationChannel::class.java,
            "FALLBACK_DEFAULT_CHANNEL_NAME"
        ).get(RetenoNotificationChannel::class.java) as String
        private val FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = Whitebox.getField(
            RetenoNotificationChannel::class.java,
            "FALLBACK_DEFAULT_CHANNEL_DESCRIPTION"
        ).get(RetenoNotificationChannel::class.java) as String

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkObject(BuildUtil)
            mockkStatic(Util::class)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkObject(BuildUtil)
            unmockkStatic(Util::class)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var contextMock: Context? = null

    private var notificationManager: NotificationManager? = null
    // endregion helper fields ---------------------------------------------------------------------


    @Throws(Exception::class)
    @Before
    override fun before() {
        super.before()
        every { BuildUtil.getTargetSdkVersion(any()) } returns 26

        contextMock = mockk()
        notificationManager = mockk()
        every { contextMock!!.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        justRun { notificationManager!!.createNotificationChannel(any()) }
    }

    @After
    override fun after() {
        super.after()

        contextMock = null
        notificationManager = null
    }

    /**
     * @see [NotificationChannelData] for expected config
     */
    @Test
    @Throws(Exception::class)
    fun givenMissingJsonConfig_whenCreateDefaultChannel_thenFallbackDefaultChannelCreated() {
        mockkObject(Reteno.Companion)
        val retenoMock = mockk<RetenoInternalImpl>()
        every { Reteno.instance } returns retenoMock
        every { retenoMock.getDefaultNotificationChannelConfig() } returns null
        // Given
        every { Util.readFromRaw(any(), any<Int>()) } throws Exception("Resource not found exception")

        val expectedChannel = NotificationChannel(
            RetenoNotificationChannel.DEFAULT_CHANNEL_ID,
            FALLBACK_DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = FALLBACK_DEFAULT_CHANNEL_DESCRIPTION
            enableLights(false)
            lightColor = 0
            enableVibration(false)
            lockscreenVisibility = 1
            setBypassDnd(false)
            setShowBadge(false)
        }

        // When
        RetenoNotificationChannel.createDefaultChannel(contextMock!!)

        // Then
        verify(exactly = 1) { notificationManager!!.createNotificationChannel(eq(expectedChannel)) }

        unmockkObject(Reteno.Companion)
    }

    /**
     * Expected config:
     *  {
     *  	"id":"defaultId",
     *  	"name":"name",
     *  	"description":"description",
     *  	"importance":4,
     *  	"enable_lights":false,
     *  	"light_color":0,
     *  	"enable_vibration":false,
     *  	"lockscreen_visibility":1,
     *  	"bypass_dnd":false,
     *  	"show_badge":true
     *  }
     */
    @Test
    @Throws(Exception::class)
    fun givenReadFromJsonConfig_whenCreateDefaultChannel_thenDefaultChannelCreated() {
        mockkObject(Reteno.Companion)
        val retenoMock = mockk<RetenoInternalImpl>()
        every { Reteno.instance } returns retenoMock
        every { retenoMock.getDefaultNotificationChannelConfig() } returns null
        // Given
        val channelJson = "{" +
                "\"id\":\"defaultId\"," +
                "\"name\":\"name\"," +
                "\"description\":\"description\"," +
                "\"importance\":4," +
                "\"enable_lights\":false," +
                "\"light_color\":0," +
                "\"enable_vibration\":false," +
                "\"lockscreen_visibility\":1," +
                "\"bypass_dnd\":false," +
                "\"show_badge\":true" +
                "}"
        every { Util.readFromRaw(any(), any()) } returns channelJson

        val expectedChannel = NotificationChannel(
            "defaultId",
            "name",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "description"
            enableLights(false)
            lightColor = 0
            enableVibration(false)
            lockscreenVisibility = 1
            setBypassDnd(false)
            setShowBadge(true)
        }

        // When
        RetenoNotificationChannel.createDefaultChannel(contextMock!!)

        // Then
        verify(exactly = 1) { notificationManager!!.createNotificationChannel(eq(expectedChannel)) }
        unmockkObject(Reteno.Companion)
    }

    /**
     * Expected config:
     *  {
     *  	"id":"SomeIdSetByClient",
     *  	"name":"someNameSetByClient",
     *  	"description":"someDescriptionSetByClient",
     *  	"importance":5,
     *  	"enable_lights":true,
     *  	"light_color":123,
     *  	"enable_vibration":true,
     *  	"lockscreen_visibility":100,
     *  	"bypass_dnd":true,
     *  	"show_badge":false
     *  }
     */
    @Test
    @Throws(Exception::class)
    fun givenCustomJsonProvided_whenCreateDefaultChannel_thenDefaultChannelCreated() {
        mockkObject(Reteno.Companion)
        val retenoMock = mockk<RetenoInternalImpl>()
        every { Reteno.instance } returns retenoMock
        every { retenoMock.getDefaultNotificationChannelConfig() } returns null
        val configJson = "{" +
                "\"id\":\"SomeIdSetByClient\"," +
                "\"name\":\"someNameSetByClient\"," +
                "\"description\":\"someDescriptionSetByClient\"," +
                "\"importance\":5," +
                "\"enable_lights\":true," +
                "\"light_color\":123," +
                "\"enable_vibration\":true," +
                "\"lockscreen_visibility\":100," +
                "\"bypass_dnd\":true," +
                "\"show_badge\":false" +
                "}"
        every { Util.readFromRaw(any(), any()) } returns configJson

        val expectedChannel = NotificationChannel(
            "SomeIdSetByClient",
            "someNameSetByClient",
            5
        ).apply {
            description = "someDescriptionSetByClient"
            enableLights(true)
            lightColor = 123
            enableVibration(true)
            lockscreenVisibility = 100
            setBypassDnd(true)
            setShowBadge(false)
        }

        // When
        RetenoNotificationChannel.createDefaultChannel(contextMock!!)

        // Then
        verify(exactly = 1) { notificationManager!!.createNotificationChannel(eq(expectedChannel)) }
        unmockkObject(Reteno.Companion)
    }

    @Test
    fun givenNotificationsEnabled_whenIsNotificationsEnabled_thenReturnTrue() {
        // Given
        every { notificationManager!!.areNotificationsEnabled() } returns true

        // When
        val isPermissionsGranted = RetenoNotificationChannel.isNotificationsEnabled(contextMock!!)

        // Then
        assertTrue(isPermissionsGranted)
    }

    @Test
    fun givenNotificationsDisabled_whenIsNotificationsEnabled_thenReturnFalse() {
        // Given
        val contextMock = mockk<Context>()
        val notificationManager = mockk<NotificationManager>()
        every { notificationManager.areNotificationsEnabled() } returns false
        every { contextMock.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        // When
        val isPermissionsGranted = RetenoNotificationChannel.isNotificationsEnabled(contextMock)

        // Then
        assertFalse(isPermissionsGranted)
    }

    @Test
    fun givenNotificationChannelIsNull_whenIsNotificationChannelEnabled_thenReturnFalse() {
        // When
        val result = RetenoNotificationChannel.isNotificationChannelEnabled(application, null)

        // Then
        assertFalse(result)
    }

    @Test
    fun givenNotificationChannelIsBlank_whenIsNotificationChannelEnabled_thenReturnFalse() {
        // When
        val result = RetenoNotificationChannel.isNotificationChannelEnabled(application, " ")

        // Then
        assertFalse(result)
    }

    @Test
    fun givenNotificationChannelIsDisabled_whenIsNotificationChannelEnabled_thenReturnFalse() {
        // Given
        mockkObject(Reteno.Companion)
        val retenoMock = mockk<RetenoInternalImpl>()
        every { Reteno.instance } returns retenoMock
        every { retenoMock.getDefaultNotificationChannelConfig() } returns null
        val channel = mockk<NotificationChannel>()
        every { channel.importance } returns NotificationManager.IMPORTANCE_NONE
        every { notificationManager!!.getNotificationChannel(DEFAULT_CHANNEL_ID) } returns channel

        // When
        val result =
            RetenoNotificationChannel.isNotificationChannelEnabled(
                contextMock!!,
                DEFAULT_CHANNEL_ID
            )

        // Then
        assertFalse(result)

        unmockkObject(Reteno.Companion)
    }

    @Test
    fun givenNotificationChannelIsEnabled_whenIsNotificationChannelEnabled_thenReturnTrue() {
        // Given
        mockkObject(Reteno.Companion)
        val retenoMock = mockk<RetenoInternalImpl>()
        every { Reteno.instance } returns retenoMock
        every { retenoMock.getDefaultNotificationChannelConfig() } returns null
        val channel = mockk<NotificationChannel>()
        every { channel.importance } returns NotificationManager.IMPORTANCE_HIGH
        every { notificationManager!!.getNotificationChannel(DEFAULT_CHANNEL_ID) } returns channel

        // When
        val result =
            RetenoNotificationChannel.isNotificationChannelEnabled(
                contextMock!!,
                DEFAULT_CHANNEL_ID
            )

        // Then
        assertTrue(result)
        unmockkObject(Reteno.Companion)
    }

    @Test
    fun givenNotificationChannelIsNull_whenIsNotificationChannelEnabled_thenCreateDefaultChannelIsCalled() {
        // Given
        every { notificationManager!!.getNotificationChannel(DEFAULT_CHANNEL_ID) } returns null

        // When
        val spySut = spyk<RetenoNotificationChannel>()
        justRun { spySut.createDefaultChannel(any()) }
        spySut.isNotificationChannelEnabled(contextMock!!, DEFAULT_CHANNEL_ID)

        // Then
        verify(exactly = 1) { spySut.createDefaultChannel(contextMock!!) }
    }
}