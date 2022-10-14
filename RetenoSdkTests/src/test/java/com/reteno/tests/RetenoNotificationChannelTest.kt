package com.reteno.tests

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.reteno.push.channel.RetenoNotificationChannel
import com.reteno.push.channel.RetenoNotificationChannel.DEFAULT_CHANNEL_ID
import com.reteno.tests._setup.FakeAndroidKeyStore.FakeKeyStore
import com.reteno.util.BuildUtil
import com.reteno.util.Util
import io.mockk.*
import junit.framework.TestCase.*
import org.junit.*
import org.junit.runners.MethodSorters
import org.powermock.reflect.Whitebox
import org.robolectric.annotation.Config
import java.security.Provider
import java.security.Security

@Config(sdk = [26])
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RetenoNotificationChannelTest : AbstractTest() {

    @Throws(Exception::class)
    @Before
    override fun before() {
        super.before()
        val provider = object : Provider("AndroidKeyStore", 1.0, "") {
            init {
                put("KeyStore.AndroidKeyStore", FakeKeyStore::class.java.name)
            }
        }
        Security.addProvider(provider)

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
    fun testA_DefaultNotificationChannelFallback() {
        assertNotNull(application)

        mockkStatic(Util::class)
        every { Util.readFromRaw(any<Int>()) } throws Exception("Resource not found exception")


        val channelName = Whitebox.getField(
            RetenoNotificationChannel::class.java,
            "FALLBACK_DEFAULT_CHANNEL_NAME"
        ).get(RetenoNotificationChannel::class.java) as String
        val channelDescription = Whitebox.getField(
            RetenoNotificationChannel::class.java,
            "FALLBACK_DEFAULT_CHANNEL_DESCRIPTION"
        ).get(RetenoNotificationChannel::class.java) as String

        val expectedChannel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = channelDescription
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
        val defaultChannelInSystem = notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID)
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
    fun testB_DefaultNotificationChannelJson() {
        assertNotNull(application)

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
        val defaultChannelInSystem = notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID)
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
    fun testC_DefaultNotificationChannelConfigured() {
        assertNotNull(application)

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

        val configJson ="{" +
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
        val defaultChannelInSystem = notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID)
        assertNotNull(defaultChannelInSystem)

        assertEquals(expectedChannel, defaultChannelInSystem)
    }



    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            com.reteno.tests._setup.FakeAndroidKeyStore.setup
        }
    }
}