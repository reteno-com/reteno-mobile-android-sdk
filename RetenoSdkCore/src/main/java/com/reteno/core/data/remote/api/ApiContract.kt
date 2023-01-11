package com.reteno.core.data.remote.api

internal sealed interface ApiContract {

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

        object InAppMessages : MobileApi() {
            // TODO: Replace with actual URL
            override val url = "${BASE_URL}inapp"
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

    sealed class Recommendation : ApiContract {
        companion object {
            private const val BASE_URL = "https://mobile-api.reteno.com/api/v1/recoms/"
        }

        class GetRecoms(recomVariantId: String) : Recommendation() {
            override val url = "${BASE_URL}$recomVariantId/request"
        }

        object PostRecoms : Recommendation() {
            override val url = "${BASE_URL}events"
        }
    }

    sealed class InAppMessages: ApiContract {
        object BaseHtml: InAppMessages() {
            override val url = "https://statics.esputnik.com/in-app/base.latest.html"
        }
    }

    data class Custom(override val url: String) : ApiContract

}