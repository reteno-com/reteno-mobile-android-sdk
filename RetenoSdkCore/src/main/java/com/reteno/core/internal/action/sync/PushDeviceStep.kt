package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.device.DeviceRemote
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.internal.action.trace.ActionRunner
import com.reteno.core.internal.action.trace.ActionTrace
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.network.ApiResult
import com.reteno.core.internal.network.SuspendApiClient
import com.reteno.core.util.Util
import kotlinx.coroutines.withContext

internal class PushDeviceStep(
    private val devicesDb: RetenoDatabaseManagerDevice,
    private val api: SuspendApiClient,
    private val configRepository: ConfigRepository,
    private val dispatchers: RetenoDispatchers,
    private val runner: ActionRunner
) : SyncStep {

    override suspend fun invoke(): Result<StepOutcome> = runner("PushDevice") {
        var outcome = pushRound()
        var round = 1
        while (outcome == RoundOutcome.MORE_PENDING && round < MAX_ROUNDS) {
            outcome = pushRound()
            round++
        }
        if (outcome == RoundOutcome.ABORT) StepOutcome.ABORT else StepOutcome.CONTINUE
    }

    private suspend fun ActionTrace.pushRound(): RoundOutcome {
        val devices = step("load devices from local database", detail = { "count=${it.size}" }) {
            withContext(dispatchers.io) { devicesDb.getDevices() }
        }
        val unsynced = devices.filter { it.isSynchronizedWithBackend != BooleanDb.TRUE }.maxByOrNull { it.createdAt }
        if (unsynced == null) {
            nothingToSend()
            return RoundOutcome.DONE
        }
        val synced = devices.filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }.maxByOrNull { it.createdAt }
        val request = requestFor(unsynced, synced)
        if (request == null) {
            step("mark unchanged device as synced in local database") { markSynced(devices, unsynced) }
            return RoundOutcome.DONE
        }
        val result = step("send device to server") {
            api.request(HttpMethod.POST, ApiContract.MobileApi.Device, request.toJson())
        }
        if (result is ApiResult.Failure && result.isRepeatable) return RoundOutcome.ABORT
        if (result is ApiResult.Success) {
            step("save device registered flag to local settings") { configRepository.saveDeviceRegistered(true) }
        }
        step("mark device as synced in local database") { markSynced(devices, unsynced) }
        val pending = step("count unsynced devices in local database", detail = { "count=$it" }) {
            withContext(dispatchers.io) { devicesDb.getUnSyncedDeviceCount() }
        }
        return if (pending > 0) RoundOutcome.MORE_PENDING else RoundOutcome.DONE
    }

    private fun requestFor(unsynced: DeviceDb, synced: DeviceDb?): DeviceRemote? {
        val remote = unsynced.toRemote()
        return when {
            synced == null -> remote
            Util.isTimestampOutdated(synced.createdAt, unsynced.createdAt) -> remote
            remote == synced.toRemote() -> null
            else -> remote
        }
    }

    private suspend fun markSynced(devices: List<DeviceDb>, latest: DeviceDb) = withContext(dispatchers.io) {
        devicesDb.deleteDevices(devices)
        devicesDb.insertDevice(latest.copy(isSynchronizedWithBackend = BooleanDb.TRUE))
    }

    private enum class RoundOutcome { DONE, MORE_PENDING, ABORT }

    companion object {
        const val MAX_ROUNDS = 5
    }
}
