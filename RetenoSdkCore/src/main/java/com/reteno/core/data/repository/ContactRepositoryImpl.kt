package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toDb
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.device.DeviceRemote
import com.reteno.core.data.remote.model.user.UserRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import com.reteno.core.util.isNonRepeatableError

internal class ContactRepositoryImpl(
    private val apiClient: ApiClient,
    private val configRepository: ConfigRepository,
    private val databaseManagerDevice: RetenoDatabaseManagerDevice,
    private val databaseManagerUser: RetenoDatabaseManagerUser
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

    override fun saveDeviceDataImmediate(device: Device) {
        onSaveDeviceData(device)
    }

    override fun saveUserData(user: User, toParallelWork: Boolean) {
        /*@formatter:off*/ Logger.i(TAG, "saveUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/
        if (toParallelWork) {
            OperationQueue.addParallelOperation { onSaveUserData(user) }
        } else {
            OperationQueue.addOperation { onSaveUserData(user) }
        }
    }

    override fun pushDeviceData() {
        val devices: List<DeviceDb> = databaseManagerDevice.getDevices()

        val latestDevice = devices.filter { it.isSynchronizedWithBackend != BooleanDb.TRUE }
            .maxByOrNull {
                it.createdAt
            }

        val latestSynchedDevice = devices.filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }
            .maxByOrNull {
                it.createdAt
            }

        val requestModel = createDeviceRequestModel(latestDevice, latestSynchedDevice)

        if (requestModel == null) {
            PushOperationQueue.nextOperation()
            return
        }

        /*@formatter:off*/ Logger.i(TAG, "pushDeviceData(): ", "device = [" , requestModel , "]")
        /*@formatter:on*/
        apiClient.postSync(
            url = ApiContract.MobileApi.Device,
            jsonBody = requestModel.toJson(),
            responseHandler = object : ResponseCallback {
                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "pushDeviceData, onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    configRepository.saveDeviceRegistered(true)
                    databaseManagerDevice.deleteDevices(devices)
                    latestDevice?.let {
                        databaseManagerDevice.insertDevice(it.copy(isSynchronizedWithBackend = BooleanDb.TRUE))
                    }
                    if (databaseManagerDevice.getUnSyncedDeviceCount() > 0) {
                        pushDeviceData()
                    } else {
                        PushOperationQueue.addOperation { pushUserData() } //User data should be pushed only after device update
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "pushDeviceData, onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManagerDevice.deleteDevices(devices)
                        latestDevice?.let {
                            databaseManagerDevice.insertDevice(it.copy(isSynchronizedWithBackend = BooleanDb.TRUE))
                        }
                        if (databaseManagerDevice.getUnSyncedDeviceCount() > 0) {
                            pushDeviceData()
                        }
                    }
                    PushOperationQueue.removeAllOperations()
                }

            })
    }

    override fun pushUserData() {
        val users: List<UserDb> = databaseManagerUser.getUsers()

        val latestUsers = users.filter { it.isSynchronizedWithBackend != BooleanDb.TRUE }

        val latestSynchedUser = users.filter { it.isSynchronizedWithBackend == BooleanDb.TRUE }
            .maxByOrNull {
                it.createdAt
            }

        val requestModel = createUserRequestModel(latestUsers, latestSynchedUser)

        if (requestModel == null) {
            databaseManagerUser.deleteUsers(latestUsers)
            PushOperationQueue.nextOperation()
            return
        }

        /*@formatter:off*/ Logger.i(TAG, "pushUserData(): ", "user = [" , requestModel , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.MobileApi.User,
            requestModel.toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    databaseManagerUser.deleteUsers(users)
                    databaseManagerUser.insertUser(requestModel.toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE))
                    PushOperationQueue.nextOperation()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManagerUser.deleteUsers(users)
                        databaseManagerUser.insertUser(requestModel.toDb().copy(isSynchronizedWithBackend = BooleanDb.TRUE))
                    } else {
                        PushOperationQueue.removeAllOperations()
                    }
                }

            })
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
        OperationQueue.addOperation { pushDeviceData() }
    }

    private fun onSaveUserData(user: User) {
        databaseManagerUser.insertUser(user.toDb(configRepository.getDeviceId()))
    }

    private fun createDeviceRequestModel(
        latestDevice: DeviceDb?,
        latestSynchedDevice: DeviceDb?
    ): DeviceRemote? {
        if (latestDevice == null) return null

        val latestDeviceRemote = latestDevice.toRemote()

        if (latestSynchedDevice == null) {
            /*@formatter:off*/ Logger.i(TAG, "pushDeviceData(): ", "No saved device found, pushing new device.")
            /*@formatter:on*/
            return latestDeviceRemote
        }

        if (Util.isTimestampOutdated(latestSynchedDevice.createdAt, latestDevice.createdAt)) {
            /*@formatter:off*/ Logger.i(TAG, "pushDeviceData(): ", "Saved device is outdated, pushing new device.")
            /*@formatter:on*/
            return latestDeviceRemote
        }

        val latestSynchedDeviceRemote = latestSynchedDevice.toRemote()

        return if (latestDeviceRemote == latestSynchedDeviceRemote) {
            null
        } else {
            latestDeviceRemote
        }
    }

    private fun createUserRequestModel(
        usersToPush: List<UserDb>,
        latestSynchedUser: UserDb?
    ): UserRemote? {
        if (usersToPush.isEmpty()) return null

        val sorted = usersToPush.sortedBy { it.createdAt }
        val firstCreatedDate = sorted.first().createdAt
        val combinedUser = sorted
            .map { it.toRemote() }
            .reduce { acc, userDb ->
                userDb.createAccModel(acc)
            }

        if (latestSynchedUser == null) {
            /*@formatter:off*/ Logger.i(TAG, "pushUserData(): ", "No saved user found, pushing new user.")
            /*@formatter:on*/
            return combinedUser
        }

        if (Util.isTimestampOutdated(latestSynchedUser.createdAt, firstCreatedDate)) {
            /*@formatter:off*/ Logger.i(TAG, "pushUserData(): ", "Saved user is outdated, pushing new user.")
            /*@formatter:on*/
            return combinedUser
        }

        val latestSynchedUserRemote = latestSynchedUser.toRemote()

        return combinedUser.createDiffModel(latestSynchedUserRemote)
    }

    companion object {
        private val TAG = ContactRepositoryImpl::class.java.simpleName
    }
}