package com.reteno.core.data.local.cache

import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.fromDb
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.util.Logger

class InteractionCacheStore(
    private val interactionManager: RetenoDatabaseManagerInteraction
) {

    fun processCachedInteractions() {
        /*@formatter:off*/ Logger.i(TAG, "processCachedInteractions(): ")
        /*@formatter:on*/
        val interactions = interactionManager.getInteractionRequests()
        interactions.forEach {
            try {
                RetenoInternalImpl.instance.recordInteraction(it.id, it.status.fromDb())
                interactionManager.deleteInteractionRequest(it)
            } catch (e: Exception) {
                /*@formatter:off*/ Logger.i(TAG, "processCachedInteractions(): ", "processing cached request failed. Interaction: $it")
                /*@formatter:on*/
            }
        }
    }

    fun recordInteraction(id: String, status: InteractionStatus) {
        /*@formatter:off*/ Logger.i(TAG, "recordInteraction(): ", "id = [", id, "]", "status = [", status, "]")
        /*@formatter:on*/
        interactionManager.insertInteractionRequest(
            InteractionRequestDb(
                id = id,
                status = status.toDb()
            )
        )
    }

    companion object {
        private val TAG: String = InteractionCacheStore::class.java.simpleName
    }
}