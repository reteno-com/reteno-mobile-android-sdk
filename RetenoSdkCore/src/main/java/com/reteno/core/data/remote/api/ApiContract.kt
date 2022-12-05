package com.reteno.core.data.remote.api

sealed interface ApiContract {

    val url: String

    sealed class RetenoApi : ApiContract {

        companion object {
            private const val BASE_URL = "https://api.reteno.com/api/v1/"
        }

        class InteractionStatus(interactionId: String) : RetenoApi() {
            override val url = "${BASE_URL}interactions/$interactionId/status"
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

    sealed class AppInbox : ApiContract {

        companion object {
            private const val BASE_URL = "https://mobile-api.reteno.com/api/v1/appinbox/"
            const val QUERY_PAGE = "page"
            const val QUERY_PAGE_SIZE = "pageSize"
        }

        object MessagesCount : AppInbox() {
            override val url = "${BASE_URL}messages/count"
        }

        object Messages : AppInbox() {
            override val url = "${BASE_URL}messages"
        }

        object MessagesStatus : AppInbox() {
            override val url = "${BASE_URL}messages/status"
        }

    }

    data class Custom(override val url: String) : ApiContract

}