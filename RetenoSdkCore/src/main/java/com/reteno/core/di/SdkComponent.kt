package com.reteno.core.di

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.reteno.core.data.local.config.DeviceIdHelper
import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.RetenoDatabaseImpl
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.data.remote.api.RestClient
import com.reteno.core.data.remote.api.RestClientImpl
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.ConfigRepositoryImpl
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.config.SdkConfiguration
import com.reteno.core.internal.config.SdkConfigurationImpl
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.coroutine.retenoScope
import com.reteno.core.internal.lifecycle.LifecycleEventFilter
import com.reteno.core.internal.lifecycle.SessionTracker
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.internal.platform.AndroidAppEnvironment
import com.reteno.core.internal.platform.AndroidBackgroundWork
import com.reteno.core.internal.platform.AndroidDeviceIdSources
import com.reteno.core.internal.platform.AndroidHostUi
import com.reteno.core.internal.platform.AppBroadcaster
import com.reteno.core.internal.platform.FirebaseFcmTokenSource
import com.reteno.core.internal.platform.lazyAppWorkManager
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoActivityHelperImpl
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.permission.PermissionActivityDelegate
import com.reteno.core.util.isOsVersionSupported
import java.util.UUID

internal class SdkComponent(
    private val application: Application,
    private val dispatchers: RetenoDispatchers,
    private val isOsSupported: () -> Boolean = ::isOsVersionSupported
) : SdkInfrastructure, FacadeInfrastructure {

    private val lifecycleEventFilter by lazy { LifecycleEventFilter(LifecycleTrackingOptions.DEFAULT) }
    override val configuration: SdkConfiguration by lazy { SdkConfigurationImpl(lifecycleEventFilter) }
    private val retenoConfig = { requireNotNull(configuration.config) }
    private val logSink by lazy { AndroidLogSink() }

    override val permissions = PermissionActivityDelegate(application, dispatchers.main)

    override val prefs by lazy { SharedPrefsManager(application) }
    private val deviceIdHelper by lazy { DeviceIdHelper(AndroidDeviceIdSources(application), prefs, retenoConfig) }
    override val restConfig by lazy { RestConfig(prefs, deviceIdHelper) }
    override val restClient: RestClient by lazy { RestClientImpl(restConfig, retenoConfig, logSink) }
    override val configRepository: ConfigRepository by lazy {
        ConfigRepositoryImpl(prefs, restConfig, FirebaseFcmTokenSource())
    }
    override val fileManager by lazy { FileManager(application, dispatchers) }
    override val activityHelper: RetenoActivityHelper by lazy { RetenoActivityHelperImpl() }
    override val db by lazy { databaseManagers(RetenoDatabaseImpl(application)) }

    override val sessionHandler: RetenoSessionHandler by lazy {
        SessionTracker(
            prefs = prefs,
            filter = lifecycleEventFilter,
            logEvent = actionGraph.events.logEvent,
            scope = actionGraph.core.scope,
            clock = System::currentTimeMillis,
            newSessionId = { UUID.randomUUID().toString() }
        )
    }

    private val broadcaster by lazy { AppBroadcaster(application) }

    private val actionGraph by lazy {
        ActionGraph(
            infrastructure = this,
            dispatchers = dispatchers,
            logSink = logSink,
            scope = retenoScope(dispatchers, logSink),
            configuration = configuration,
            environment = AndroidAppEnvironment(application),
            broadcasts = broadcaster,
            hostUi = AndroidHostUi(application, ProcessLifecycleOwner.get(), activityHelper, broadcaster),
            backgroundWork = AndroidBackgroundWork(application, lazyAppWorkManager(application)),
            lifecycleEventFilter = lifecycleEventFilter,
            clock = System::currentTimeMillis
        ).also { it.installLogging() }
    }

    override val actions: SdkActions by lazy {
        if (isOsSupported()) actionGraph.router else UnsupportedOsActions(configuration)
    }
}
