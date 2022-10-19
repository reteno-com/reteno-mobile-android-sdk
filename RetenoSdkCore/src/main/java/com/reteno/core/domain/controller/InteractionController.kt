package com.reteno.core.domain.controller

import com.reteno.core.data.local.ds.ConfigRepository
import com.reteno.core.data.remote.ds.InteractionRepository
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.model.interaction.Interaction
import com.reteno.core.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import java.time.Instant
import java.time.temporal.ChronoUnit

class InteractionController(private val configRepository: ConfigRepository, private val interactionRepository: InteractionRepository) {

    fun onInteraction(interactionId: String, status: InteractionStatus) {
        val fcmToken = configRepository.getFcmToken()
        val currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val timeStamp = currentDate.toString()
        val interaction = Interaction(status, timeStamp, fcmToken)
        /*@formatter:off*/ Logger.i(TAG, "onInteraction(): ", "interactionId = [" , interactionId , "], interaction = [" , interaction.toString() , "]")
        /*@formatter:on*/
        sendInteraction(interactionId, interaction)
    }

    private fun sendInteraction(interactionId: String, interaction: Interaction) {
        interactionRepository.sendInteraction(interactionId, interaction, object :
            ResponseCallback {
            override fun onSuccess(response: String) {
                /*@formatter:off*/ Logger.i(ContactController.TAG, "onSuccess(): ", "response = [" , response , "]")
                /*@formatter:on*/
            }

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                /*@formatter:off*/ Logger.i(ContactController.TAG, "onFailure(): ", "statusCode = [" , statusCode , "], response = [" , response , "], throwable = [" , throwable , "]")
                /*@formatter:on*/
            }

        })
    }

    companion object {
        val TAG: String = InteractionController::class.java.simpleName
    }
}