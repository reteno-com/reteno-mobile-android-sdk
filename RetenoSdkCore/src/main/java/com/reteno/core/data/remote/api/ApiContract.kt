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
        companion object {
            private const val BASE_URL = "https://mobile-api.reteno.com/api/v1/inapp/"
        }

        object BaseHtml: InAppMessages() {
            override val url = "https://statics.esputnik.com/in-app/base.latest.html"
        }

        class GetInnAppWidgetByInteractionId(interactionId:String): InAppMessages(){
            override val url = "${BASE_URL}interactions/$interactionId/message"
        }

        object WidgetInitFailed: InAppMessages() {
            override val url = "https://site-script.reteno.com/site-script/v1/event"
        }

        object GetInAppMessages: InAppMessages() {
            override val url = "${BASE_URL}messages"
        }

        object GetInAppMessagesContent: InAppMessages() {
            override val url = "${BASE_URL}contents/request"
        }

        object CheckUserInSegments: InAppMessages() {
            override val url = "${BASE_URL}async-rules/check"
        }

        object RegisterInteraction: InAppMessages() {
            override val url = "https://mobile-api.reteno.com/api/v1/interaction"
        }
    }

    sealed class LogEvent : ApiContract {

        companion object {
            private const val BASE_URL = "https://mobile-api.reteno.com/logs/v1/"
        }

        object Events : LogEvent() {
            override val url = "${BASE_URL}events"
        }
    }

    data class Custom(override val url: String) : ApiContract

}