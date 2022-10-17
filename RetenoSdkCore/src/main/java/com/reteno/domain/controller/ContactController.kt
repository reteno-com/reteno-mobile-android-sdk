package com.reteno.domain.controller

import com.reteno.data.remote.ds.ContactRepository
import com.reteno.domain.ResponseCallback
import com.reteno.model.device.Device
import com.reteno.util.Logger

class ContactController(private val contactRepository: ContactRepository) {

    fun onNewContact(contact: Device) {
        contactRepository.sendDeviceProperties(contact, object : ResponseCallback {
            override fun onSuccess(response: String) {
                /*@formatter:off*/ Logger.i(TAG, "onSuccess(): ", "response = [" , response , "]")
                /*@formatter:on*/
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                /*@formatter:off*/ Logger.i(TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                /*@formatter:on*/
            }

        })
    }

    companion object {
        val TAG: String = ContactController::class.java.simpleName
    }
}