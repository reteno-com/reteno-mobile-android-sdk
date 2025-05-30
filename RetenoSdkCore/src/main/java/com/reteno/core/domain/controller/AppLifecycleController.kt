package com.reteno.core.domain.controller

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleEvent
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoLifecycleCallBacksAdapter
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.lifecycle.RetenoSessionHandler.SessionEvent
import com.reteno.core.util.Constants
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.asZonedDateTime
import com.reteno.core.util.Util.toTypeMap
import com.reteno.core.util.queryBroadcastReceivers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

class AppLifecycleController internal constructor(
    private val configRepository: ConfigRepository,
    private val eventController: EventController,
    private val sessionHandler: RetenoSessionHandler,
    private val scheduleController: ScheduleController,
    private val iamController: IamController,
    activityHelper: RetenoActivityHelper,
    lifecycleTrackingOptions: LifecycleTrackingOptions,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : LifecycleObserver {

    private var wasBackgrounded = false
    private var isStarted = false
    private var appOpenedTimestamp = System.currentTimeMillis()
    private var lifecycleEventConfig = lifecycleTrackingOptions.toTypeMap()
    private val lifecycleCallbacks = RetenoLifecycleCallBacksAdapter(onPause = ::onActivityPause)

    init {
        sessionHandler.sessionEventFlow
            .onEach { handleSessionEvent(it) }
            .launchIn(scope)
        configRepository.notificationState
            .drop(1)
            .onEach { notifyNotificationsStateChanged(it) }
            .launchIn(scope)
        activityHelper.registerActivityLifecycleCallbacks(TAG, lifecycleCallbacks)
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        sendAppResumeBroadcast()
        scheduleController.clearOldData()
        initMetadata()
        iamController.preloadHtml()
        if (isStarted) return
        isStarted = true
        trackLifecycleEvent(Event.applicationOpen(wasBackgrounded))
        appOpenedTimestamp = System.currentTimeMillis()
        wasBackgrounded = false
    }

    @MainThread
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        if (!isStarted) return
        isStarted = false
        wasBackgrounded = true
        trackLifecycleEvent(
            Event.applicationBackgrounded(
                appOpenedTimestamp,
                sessionHandler.getForegroundTimeMillis().milliseconds.inWholeSeconds
            )
        )
    }

    fun setLifecycleEventConfig(lifecycleEventConfig: LifecycleTrackingOptions) {
        /*@formatter:off*/ Logger.i(TAG, "setLifecycleEventConfig(): ", "lifecycleEventConfig = [" , lifecycleEventConfig , "]")
        /*@formatter:on*/
        this.lifecycleEventConfig = lifecycleEventConfig.toTypeMap()
    }

    fun initMetadata() {
        val savedAppVersion = configRepository.getAppVersion()
        val savedAppBuild = configRepository.getAppBuildNumber()
        val (version, code) = try {
            val pInfo = configRepository.getAppPackageInfo()
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
            pInfo.versionName.orEmpty() to versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "" to 0L
        }
        when {
            savedAppVersion.isEmpty() -> {
                trackLifecycleEvent(Event.applicationInstall(version, code))
                configRepository.saveAppVersion(version)
                configRepository.saveAppBuildNumber(code)
            }

            savedAppVersion != version || savedAppBuild != code -> {
                trackLifecycleEvent(
                    Event.applicationUpdate(
                        version = version,
                        build = code,
                        prevVersion = savedAppVersion,
                        prevBuild = savedAppBuild
                    )
                )
                configRepository.saveAppVersion(version)
                configRepository.saveAppBuildNumber(code)
            }
        }
    }

    private fun onActivityPause(activity: Activity) {
        runCatching {
            val activityManager =
                (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            val topComponentInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activityManager.appTasks.first().taskInfo.topActivity
            } else {
                activityManager.getRunningTasks(1)[0].topActivity
            }
            if (topComponentInfo?.packageName == PERMISSION_DIALOG_PACKAGE) {
                trackLifecycleEvent(Event.permissionDialogDisplayed())
            }
        }
    }

    private fun notifyNotificationsStateChanged(notificationsEnabled: Boolean) {
        val event = if (notificationsEnabled) {
            Event.notificationsEnabled()
        } else {
            Event.notificationsDisabled()
        }
        trackLifecycleEvent(event)
    }

    private fun handleSessionEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.SessionEndEvent -> trackLifecycleEvent(
                Event.sessionEnd(
                    event.sessionId,
                    event.endTime.asZonedDateTime(),
                    event.durationInMillis.milliseconds.inWholeSeconds.toInt(),
                    event.openCount,
                    event.bgCount
                )
            )

            is SessionEvent.SessionStartEvent -> trackLifecycleEvent(
                Event.sessionStart(event.sessionId, event.startTime.asZonedDateTime())
            )
        }
    }


    private fun trackLifecycleEvent(lifecycleEvent: LifecycleEvent) {
        if (lifecycleEventConfig.getOrElse(lifecycleEvent.type) { false }) {
            eventController.trackEvent(lifecycleEvent.event)
        }
    }

    private fun sendAppResumeBroadcast() {
        val intent =
            Intent(Constants.BROADCAST_ACTION_RETENO_APP_RESUME).setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val infoList = RetenoInternalImpl.instance.application.queryBroadcastReceivers(intent)
        for (info in infoList) {
            info?.activityInfo?.let {
                intent.component = ComponentName(it.packageName, it.name)
                RetenoInternalImpl.instance.application.sendBroadcast(intent)
            }
        }
    }

    companion object {
        private const val PERMISSION_DIALOG_PACKAGE = "com.google.android.permissioncontroller"
        private val TAG: String = AppLifecycleController::class.java.simpleName
    }
}