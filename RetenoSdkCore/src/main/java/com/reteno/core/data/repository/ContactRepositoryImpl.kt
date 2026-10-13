package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User
import com.reteno.core.util.Logger

internal class ContactRepositoryImpl(
    private val configRepository: ConfigRepository,
    private val databaseManagerDevice: RetenoDatabaseManagerDevice,
    private val databaseManagerUser: RetenoDatabaseManagerUser,
    private val requestSync: () -> Unit
) : ContactRepository {

    override fun saveDeviceData(device: Device, toParallelWork: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "device = [" , device , "]")
        /*@formatter:on*/
        if (toParallelWork) {
            OperationQueue.addParallelOperation { onSaveDeviceData(device) }
        } else {
            OperationQueue.addOperation { onSaveDeviceData(device) }
        }
    }

    override suspend fun saveDeviceData(device: Device) {
        /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "device = [" , device , "]")
        /*@formatter:on*/

        val newDevice: DeviceDb = device.toDb()
        val savedDevices: List<DeviceDb> = databaseManagerDevice.getDevices()
        val mappedSavedDevices = savedDevices.map {
            it.copy(
                rowId = null,
                createdAt = 0L,
                isSynchronizedWithBackend = null
            )
        }

        if (mappedSavedDevices.contains(newDevice).not()) {
            databaseManagerDevice.insertDevice(device.toDb())
            /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "Device saved")
            /*@formatter:on*/
        } else {
            /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "Device NOT saved. Device is already present in database. Duplicates are not saved")
            /*@formatter:on*/
        }
    }

    override fun saveDeviceDataImmediate(device: Device) {
        onSaveDeviceData(device)
    }

    @Deprecated("Use suspend alternative")
    override fun saveUserDataDeprecated(user: User, toParallelWork: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "saveUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/
        if (toParallelWork) {
            OperationQueue.addParallelOperation { onSaveUserData(user) }
        } else {
            OperationQueue.addOperation { onSaveUserData(user) }
        }
    }

    override suspend fun saveUserData(user: User) {
        databaseManagerUser.insertUser(user.toDb(configRepository.getDeviceId()))
    }

    override fun deleteSynchedDevices() {
        val devices: List<DeviceDb> = databaseManagerDevice.getDevices()
        val synchedDevices = devices.filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }
        if (synchedDevices.isNotEmpty()) {
            databaseManagerDevice.deleteDevices(synchedDevices)
        }
    }

    private fun onSaveDeviceData(device: Device) {
        val newDevice: DeviceDb = device.toDb()
        val savedDevices: List<DeviceDb> = databaseManagerDevice.getDevices()
        val mappedSavedDevices = savedDevices.map {
            it.copy(
                rowId = null,
                createdAt = 0L,
                isSynchronizedWithBackend = null
            )
        }

        if (mappedSavedDevices.contains(newDevice).not()) {
            databaseManagerDevice.insertDevice(device.toDb())
            /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "Device saved")
            /*@formatter:on*/
        } else {
            /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "Device NOT saved. Device is already present in database. Duplicates are not saved")
            /*@formatter:on*/
        }
        requestSync()
    }

    private fun onSaveUserData(user: User) {
        databaseManagerUser.insertUser(user.toDb(configRepository.getDeviceId()))
    }

    companion object {
        private val TAG = ContactRepositoryImpl::class.java.simpleName
    }
}