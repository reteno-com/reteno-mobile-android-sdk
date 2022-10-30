package com.reteno.core.domain.controller

import com.reteno.core.data.repository.DeeplinkRepository

class DeeplinkController(private val deeplinkRepository: DeeplinkRepository) {

    fun triggerDeeplinkClicked(wrappedLink: String, unwrappedLink: String) {
        deeplinkRepository.triggerWrappedLinkClicked(wrappedLink)
    }
}