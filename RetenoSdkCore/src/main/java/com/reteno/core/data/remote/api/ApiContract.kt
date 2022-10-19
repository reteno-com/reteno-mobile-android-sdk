package com.reteno.core.data.remote.api

sealed interface ApiContract {

    val url: String

    sealed class RetenoApi : ApiContract {

        companion object {
            private const val BASE_URL = "https://api.reteno.com/api/v1/"
        }

        class InteractionStatus(interactionId: String) : RetenoApi() {
            override val url = "${BASE_URL}/interactions/$interactionId/status"
        }

        object EventStatus : RetenoApi() { // TODO replace with pushEventStatus endpoint
            override val url = "${BASE_URL}contact"
        }

    }

    sealed class MobileApi : ApiContract {

        companion object {
            private const val BASE_URL = "https://mobile-api.reteno.com/api/v1/"
        }

        object Device : MobileApi() {
            override val url = "${BASE_URL}device"
        }

        object User : MobileApi() {
            override val url = "${BASE_URL}user"
        }

        object Events : MobileApi() {
            override val url = "${BASE_URL}events"
        }

        object Configuration : MobileApi() {
            override val url = "${BASE_URL}configuration"
        }
    }

    data class Custom(override val url: String) : ApiContract

}