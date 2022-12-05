package com.reteno.core.domain.controller

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.Util

class InteractionController(private val configRepository: ConfigRepository, private val interactionRepository: InteractionRepository) {

    fun onInteraction(interactionId: String, status: InteractionStatus) {
        val fcmToken = configRepository.getFcmToken()
        if (fcmToken.isBlank()) {
            /*@formatter:off*/ Logger.i(TAG, "onInteraction(): ", "interactionId = [" , interactionId , "], NO PUSH TOKEN FOUND. Terminating")
            /*@formatter:on*/
            return
        }
        val timeStamp = Util.getCurrentTimeStamp()
        val interaction = Interaction(status, timeStamp, fcmToken)
        /*@formatter:off*/ Logger.i(TAG, "onInteraction(): ", "interactionId = [" , interactionId , "], interaction = [" , interaction.toString() , "]")
        /*@formatter:on*/
        saveInteraction(interactionId, interaction)
    }

    fun pushInteractions() {
        /*@formatter:off*/ Logger.i(TAG, "pushInteractions(): ", "")
        /*@formatter:on*/
        interactionRepository.pushInteractions()
    }

    private fun saveInteraction(interactionId: String, interaction: Interaction) {
        interactionRepository.saveInteraction(interactionId, interaction)
    }

    fun clearOldInteractions() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldInteractions(): ", "")
        /*@formatter:on*/
        val outdatedTime = SchedulerUtils.getOutdatedTime()
        interactionRepository.clearOldInteractions(outdatedTime)
    }

    companion object {
        private val TAG: String = InteractionController::class.java.simpleName
    }
}