package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.device.Device
import com.reteno.core.domain.model.user.User
import com.reteno.core.util.Logger
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
        val device: DeviceDb = databaseManagerDevice.getDevices(1).firstOrNull() ?: kotlin.run {
            PushOperationQueue.nextOperation()
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pushDeviceData(): ", "device = [" , device , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.MobileApi.Device,
            device.toRemote().toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    configRepository.saveDeviceRegistered(true)
                    val cacheUpdated = databaseManagerDevice.deleteDevice(device)
                    if (cacheUpdated) {
                        pushDeviceData()
                    } else {
                        PushOperationQueue.nextOperation()
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        val cacheUpdated = databaseManagerDevice.deleteDevice(device)
                        if (cacheUpdated) {
                            pushDeviceData()
                        }
                    }
                    PushOperationQueue.removeAllOperations()
                }

            })
    }

    override fun pushUserData() {
        val user: UserDb = databaseManagerUser.getUsers(1).firstOrNull() ?: kotlin.run {
            PushOperationQueue.nextOperation()
            return
        }
        /*@formatter:off*/ Logger.i(TAG, "pushUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/
        apiClient.post(
            ApiContract.MobileApi.User,
            user.toRemote().toJson(),
            object : ResponseCallback {
                override fun onSuccess(response: String) {
                    /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                    /*@formatter:on*/
                    val cacheUpdated = databaseManagerUser.deleteUser(user)
                    if (cacheUpdated) {
                        pushUserData()
                    } else {
                        PushOperationQueue.nextOperation()
                    }
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        val cacheUpdated = databaseManagerUser.deleteUser(user)
                        if (cacheUpdated) {
                            pushUserData()
                        }
                    }
                    PushOperationQueue.removeAllOperations()
                }

            })
    }

    private fun onSaveDeviceData(device: Device) {
        val newDevice: DeviceDb = device.toDb()
        val savedDevices: List<DeviceDb> = databaseManagerDevice.getDevices()
        if (!savedDevices.map { it.copy(rowId = null) }.contains(newDevice)) {
            databaseManagerDevice.insertDevice(device.toDb())
            /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "Device saved")
            /*@formatter:on*/
        } else {
            /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "Device NOT saved. Device is already present in database. Duplicates are not saved")
            /*@formatter:on*/
        }
        pushDeviceData()
    }

    private fun onSaveUserData(user: User) {
        databaseManagerUser.insertUser(user.toDb(configRepository.getDeviceId()))
        pushUserData()
    }

    companion object {
        private val TAG = ContactRepositoryImpl::class.java.simpleName
    }
}