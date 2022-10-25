package com.reteno.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Util
import com.reteno.push.base.robolectric.BaseRobolectricTest
import com.reteno.push.channel.RetenoNotificationChannel
import io.mockk.*
import junit.framework.TestCase.*
import org.junit.*
import org.junit.runners.MethodSorters
import org.powermock.reflect.Whitebox
import org.robolectric.annotation.Config


@Config(sdk = [26])
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RetenoNotificationChannelTest : BaseRobolectricTest() {

    @Throws(Exception::class)
    @Before
    override fun before() {
        super.before()
        mockkObject(BuildUtil)
        every { BuildUtil.getTargetSdkVersion() } returns 26
    }

    @After
    override fun after() {
        super.after()
        unmockkObject(BuildUtil)
    }

    /**
     * @see [NotificationChannelData] for expected config
     */
    @Test
    @Throws(Exception::class)
    fun testA_givenMissingJsonConfig_whenCreateDefaultChannel_thenFallbackDefaultChannelCreated() {
        mockkStatic(Util::class)
        every { Util.readFromRaw(any<Int>()) } throws Exception("Resource not found exception")

        val expectedChannel = NotificationChannel(
            RetenoNotificationChannel.DEFAULT_CHANNEL_ID,
            FALLBACK_DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = FALLBACK_DEFAULT_CHANNEL_DESCRIPTION
            enableLights(false)
            lightColor = 0
            enableVibration(false)
            lockscreenVisibility = 1
            setBypassDnd(false)
            setShowBadge(false)
        }

        RetenoNotificationChannel.createDefaultChannel()
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultChannelInSystem = notificationManager.getNotificationChannel(
            RetenoNotificationChannel.DEFAULT_CHANNEL_ID
        )
        assertNotNull(defaultChannelInSystem)

        assertEquals(expectedChannel, defaultChannelInSystem)

        unmockkStatic(Util::class)
    }

    /**
     * Expected config:
     *  {
     *  	"id":"defaultId",
     *  	"name":"name",
     *  	"description":"description",
     *  	"importance":3,
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
    fun testB_givenReadFromJsonConfig_whenCreateDefaultChannel_thenDefaultChannelCreated() {
        val expectedChannel = NotificationChannel(
            "defaultId",
            "name",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "description"
            enableLights(false)
            lightColor = 0
            enableVibration(false)
            lockscreenVisibility = 1
            setBypassDnd(false)
            setShowBadge(true)
        }

        RetenoNotificationChannel.createDefaultChannel()
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultChannelInSystem = notificationManager.getNotificationChannel(
            RetenoNotificationChannel.DEFAULT_CHANNEL_ID
        )
        assertNotNull(defaultChannelInSystem)

        assertEquals(expectedChannel, defaultChannelInSystem)
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
    fun testC_givenCustomJsonProvided_whenCreateDefaultChannel_thenDefaultChannelCreated() {
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
        RetenoNotificationChannel.configureDefaultNotificationChannel(configJson)

        RetenoNotificationChannel.createDefaultChannel()
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultChannelInSystem = notificationManager.getNotificationChannel(
            RetenoNotificationChannel.DEFAULT_CHANNEL_ID
        )
        assertNotNull(defaultChannelInSystem)

        assertEquals(expectedChannel, defaultChannelInSystem)
    }

    companion object {
        private val FALLBACK_DEFAULT_CHANNEL_NAME = Whitebox.getField(
            RetenoNotificationChannel::class.java,
            "FALLBACK_DEFAULT_CHANNEL_NAME"
        ).get(RetenoNotificationChannel::class.java) as String
        private val FALLBACK_DEFAULT_CHANNEL_DESCRIPTION = Whitebox.getField(
            RetenoNotificationChannel::class.java,
            "FALLBACK_DEFAULT_CHANNEL_DESCRIPTION"
        ).get(RetenoNotificationChannel::class.java) as String
    }
}