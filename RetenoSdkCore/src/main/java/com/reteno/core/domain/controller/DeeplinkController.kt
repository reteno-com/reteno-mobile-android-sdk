package com.reteno.core.domain.controller

import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.util.Logger

class DeeplinkController(private val deeplinkRepository: DeeplinkRepository) {

    fun deeplinkClicked(wrappedLink: String, unwrappedLink: String) {
        /*@formatter:off*/ Logger.i(TAG, "deeplinkClicked(): ", "wrappedLink = [", wrappedLink, "], unwrappedLink = [", unwrappedLink, "]")
        /*@formatter:on*/
        deeplinkRepository.saveWrappedLink(wrappedLink)
        deeplinkRepository.pushWrappedLink()
    }

    fun pushDeeplink() {
        /*@formatter:off*/ Logger.i(TAG, "pushDeeplink(): ", "")
        /*@formatter:on*/
        deeplinkRepository.pushWrappedLink()
    }

    fun clearOldDeeplinks() {
        /*@formatter:off*/ Logger.i(TAG, "clearOldDeeplinks(): ", "")
        /*@formatter:on*/
        val outdatedTime = SchedulerUtils.getOutdatedTime()
        deeplinkRepository.clearOldWrappedLinks(outdatedTime)
    }

    companion object {
        private val TAG: String = DeeplinkController::class.java.simpleName
    }
}