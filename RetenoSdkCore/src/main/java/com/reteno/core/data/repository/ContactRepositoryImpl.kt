package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerDevice
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
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

class ContactRepositoryImpl(
    private val apiClient: ApiClient,
    private val configRepository: ConfigRepository,
    private val databaseManagerDevice: RetenoDatabaseManagerDevice,
    private val databaseManagerUser: RetenoDatabaseManagerUser
) : ContactRepository {

    override fun saveDeviceData(device: Device) {
        /*@formatter:off*/ Logger.i(TAG, "saveDeviceData(): ", "device = [" , device , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            databaseManagerDevice.insertDevice(device.toDb())
            pushDeviceData()
        }
    }

    override fun saveUserData(user: User) {
        /*@formatter:off*/ Logger.i(TAG, "saveUserData(): ", "user = [" , user , "]")
        /*@formatter:on*/
        OperationQueue.addOperation {
            databaseManagerUser.insertUser(user.toDb(configRepository.getDeviceId()))
            pushUserData()
        }
    }

    override fun pushDeviceData() {
        val device = databaseManagerDevice.getDevices(1).firstOrNull() ?: kotlin.run {
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
                    databaseManagerDevice.deleteDevices(1)
                    pushDeviceData()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManagerDevice.deleteDevices(1)
                        pushDeviceData()
                    }
                    PushOperationQueue.removeAllOperations()
                }

            })
    }

    override fun pushUserData() {
        val user = databaseManagerUser.getUser(1).firstOrNull() ?: kotlin.run {
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
                    databaseManagerUser.deleteUsers(1)
                    pushUserData()
                }

                override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                    /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                    /*@formatter:on*/
                    if (isNonRepeatableError(statusCode)) {
                        databaseManagerUser.deleteUsers(1)
                        pushUserData()
                    }
                    PushOperationQueue.removeAllOperations()
                }

            })
    }

    companion object {
        private val TAG = ContactRepositoryImpl::class.java.simpleName
    }
}