package com.reteno.core.domain.controller

import com.reteno.core.data.repository.DeeplinkRepository
import com.reteno.core.util.Logger

class DeeplinkController(private val deeplinkRepository: DeeplinkRepository) {

    fun deeplinkClicked(wrappedLink: String, unwrappedLink: String) {
        /*@formatter:off*/ Logger.i(TAG, "deeplinkClicked(): ", "wrappedLink = [", wrappedLink, "], unwrappedLink = [", unwrappedLink, "]")
        /*@formatter:on*/
        deeplinkRepository.saveWrappedLink(wrappedLink)
    }

    companion object {
        private val TAG: String = DeeplinkController::class.java.simpleName
    }
}