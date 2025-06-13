package com.reteno.core.domain.controller

import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.data.repository.InteractionRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger
import com.reteno.core.util.Util

class InteractionController(
    private val configRepository: ConfigRepository,
    private val interactionRepository: InteractionRepository,
) {

    fun onInteractionIamClick(interactionId: String, action: InteractionAction) {
        /*@formatter:off*/ Logger.i(TAG, "onClickInteraction(): ", "interactionId = [" , interactionId , "],","action = [",action,"]")
        /*@formatter:on*/
        val timeStamp = Util.getCurrentTimeStamp()
        val interaction = Interaction(InteractionStatus.CLICKED, timeStamp, action = action)
        saveInteraction(interactionId, interaction)
    }

    suspend fun onInteraction(interactionId: String, status: InteractionStatus) {
        val pushToken = configRepository.getFcmToken()
        pushToken.takeIf { it.isNotBlank() }?.run {
            val timeStamp = Util.getCurrentTimeStamp()
            val interaction = Interaction(status, timeStamp, token = pushToken)
            /*@formatter:off*/ Logger.i(TAG, "onInteraction(): ", "interactionId = [", interactionId, "], interaction = [", interaction.toString(), "]")
            /*@formatter:on*/
            saveInteraction(interactionId, interaction)
        } ?: run {
            /*@formatter:off*/ Logger.i(TAG, "onInteraction(): ", "interactionId = [", interactionId, "], NO PUSH TOKEN FOUND. Terminating")
            /*@formatter:on*/
        }
    }

    fun onInAppInteraction(inAppInteraction: InAppInteraction) {
        /*@formatter:off*/ Logger.i(TAG, "onInAppInteraction(): ", "inAppInteraction = [", inAppInteraction, "]")
        /*@formatter:on*/
        interactionRepository.saveAndPushInAppInteraction(inAppInteraction)
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
        interactionRepository.clearOldInAppInteractions(outdatedTime)
    }

    companion object {
        private val TAG: String = InteractionController::class.java.simpleName
    }
}