package com.reteno.push.interceptor.click

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import com.reteno.core.util.getResolveInfoList
import com.reteno.push.Constants.KEY_ES_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_ES_LINK_WRAPPED
import com.reteno.push.base.robolectric.BaseRobolectricTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase
import junit.framework.TestCase.*
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@Config(sdk = [26])
class IntentHandlerTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEEPLINK_WRAPPED = "https://wrapped.com"
        private const val DEEPLINK_UNWRAPPED = "https://unwrapped.com"

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic(Context::getResolveInfoList)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(Context::getResolveInfoList)
        }
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenUnwrappedDeeplink_whenGetDeeplinkIntent_thenAppLaunchIntentWithUnwrappedDeeplinkReturned() {
        // Given
        val bundle = Bundle().apply {
            putString(KEY_ES_LINK_WRAPPED, DEEPLINK_WRAPPED)
            putString(KEY_ES_LINK_UNWRAPPED, DEEPLINK_UNWRAPPED)
        }

        val expectedIntent = Intent(Intent.ACTION_VIEW, Uri.parse(DEEPLINK_UNWRAPPED)).apply {
            putExtras(bundle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // When
        val actualIntent = IntentHandler.getDeepLinkIntent(bundle)

        // Then
        assertNotNull(actualIntent)
        assertEquals(expectedIntent.toString(), actualIntent.toString())
    }

    @Test
    fun givenWrappedDeeplink_whenGetDeeplinkIntent_thenAppLaunchIntentWithWrappedDeeplinkReturned() {
        // Given
        val bundle = Bundle().apply {
            putString(KEY_ES_LINK_WRAPPED, DEEPLINK_WRAPPED)
        }

        val expectedIntent = Intent(Intent.ACTION_VIEW, Uri.parse(DEEPLINK_WRAPPED)).apply {
            putExtras(bundle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // When
        val actualIntent = IntentHandler.getDeepLinkIntent(bundle)

        // Then
        assertNotNull(actualIntent)
        assertEquals(expectedIntent.toString(), actualIntent.toString())
    }

    @Test
    fun givenNullForDeeplink_whenGetDeeplinkIntent_thenNullReturned() {
        // Given
        val bundle = Bundle()

        // When
        val actualIntent = IntentHandler.getDeepLinkIntent(bundle)

        // Then
        assertNull(actualIntent)
    }

    @Test
    fun givenAppInstalled_whenResolveAppLaunchIntent_thenAppLaunchIntentStartsApplication() {
        // Given
        val injectedIntent = injectAppLaunchIntent()

        // When
        val appLaunchIntent = IntentHandler.AppLaunchIntent.getAppLaunchIntent(application)
        application.startActivity(appLaunchIntent)
        val shadow = Shadows.shadowOf(application)
        val actualIntent = shadow.peekNextStartedActivity()
        TestCase.assertNotNull(actualIntent)

        // Then
        assertEquals(injectedIntent.`package`, actualIntent.`package`)
        assertEquals(injectedIntent.categories, actualIntent.categories)
        assertEquals(injectedIntent.action, actualIntent.action)
    }


    // region helper methods -----------------------------------------------------------------------
    private fun injectAppLaunchIntent(): Intent {
        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.setPackage(application.packageName)

        val resolveInfo = ResolveInfo()
        val activityInfo = ActivityInfo()
        activityInfo.packageName = application.packageName
        activityInfo.name = "MainActivity"
        resolveInfo.activityInfo = activityInfo

        val shadowPackageManager = Shadows.shadowOf(application.packageManager)
        shadowPackageManager.addResolveInfoForIntent(launchIntent, resolveInfo)

        return launchIntent
    }
    // endregion helper methods --------------------------------------------------------------------
}