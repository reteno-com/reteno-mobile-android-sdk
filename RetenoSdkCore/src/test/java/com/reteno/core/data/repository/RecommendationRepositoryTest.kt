package com.reteno.core.data.repository

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.data.local.database.RetenoDatabaseManager
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.remote.api.ApiClient
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.mapper.toJson
import com.reteno.core.data.remote.mapper.toRemote
import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.data.remote.model.recommendation.get.Recoms
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.domain.model.recommendation.get.RecomRequest
import com.reteno.core.domain.model.recommendation.post.RecomEvent
import com.reteno.core.domain.model.recommendation.post.RecomEventType
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.recommendation.GetRecommendationResponseCallback
import com.reteno.core.util.Logger
import com.reteno.core.util.Util.formatToRemote
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class RecommendationRepositoryTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val RECOM_VARIANT_ID = "r111v322"
        private val RECOM_PRODUCTS: List<String> = listOf("product1, product2, product3")
        private const val RECOM_CATEGORY: String = "category1"
        private val RECOM_FIELDS: List<String> = listOf("field1", "field2", "field3")

        private const val PRODUCT_ID = "w12345s1345"
        private val RECOM_EVENT_TYPE = RecomEventType.CLICKS
        private val RECOM_EVENT_TYPE_DB = RecomEventTypeDb.CLICKS

        private const val ERROR_CODE_NON_REPEATABLE = 400
        private const val ERROR_CODE_REPEATABLE = 500
        private const val ERROR_MSG = "error_msg"
        private val ERROR_EXCEPTION = MockKException(ERROR_MSG)
    }
    // endregion constants -------------------------------------------------------------------------


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var databaseManager: RetenoDatabaseManager

    @RelaxedMockK
    private lateinit var apiClient: ApiClient

    @RelaxedMockK
    private lateinit var scheduler: ScheduledExecutorService

    private lateinit var SUT: RecommendationRepositoryImpl
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockkStatic(Executors::class)
        mockkObject(PushOperationQueue)
        every { Executors.newScheduledThreadPool(any(), any()) } returns scheduler

        SUT = RecommendationRepositoryImpl(databaseManager, apiClient)
    }

    override fun after() {
        super.after()
        unmockkObject(PushOperationQueue)
        unmockkStatic(Executors::class)
    }


    @Test
    fun givenHandleResponseJsonFail_whenGetRecommendations_thenOnSuccessFallbackToJson() {
        // Given
        val recomRequest = RecomRequest(RECOM_PRODUCTS, RECOM_CATEGORY, RECOM_FIELDS)
        val responseCallback =
            mockk<GetRecommendationResponseCallback<RecommendationResponseFull>>(relaxed = true)
        val resultJson = "{}"

        val expectedApiContract = ApiContract.Recommendation.Get(RECOM_VARIANT_ID)
        val apiContractCaptured = slot<ApiContract>()
        every {
            apiClient.post(
                url = capture(apiContractCaptured),
                jsonBody = any(),
                responseHandler = any()
            )
        } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(resultJson)
        }

        // When
        SUT.getRecommendation(
            RECOM_VARIANT_ID,
            recomRequest,
            RecommendationResponseFull::class.java,
            responseCallback
        )

        // Then
        assertEquals(expectedApiContract.url, apiContractCaptured.captured.url)
        verify(exactly = 1) {
            apiClient.post(
                any(),
                eq(recomRequest.toRemote().toJson()),
                any()
            )
        }

        verify(exactly = 0) { responseCallback.onSuccess(any()) }
        verify(exactly = 1) { responseCallback.onSuccessFallbackToJson(resultJson) }
        verify(exactly = 0) { responseCallback.onFailure(any(), any(), any()) }

        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
    }

    @Test
    fun givenHandleResponseJsonSuccess_whenGetRecommendations_thenOnSuccessFallbackToJson() {
        // Given
        val recomRequest = RecomRequest(RECOM_PRODUCTS, RECOM_CATEGORY, RECOM_FIELDS)
        val responseCallback =
            mockk<GetRecommendationResponseCallback<RecommendationResponseFull>>(relaxed = true)
        val result =
            Recoms(listOf(getRecommendationResponseFull_1(), getRecommendationResponseFull_2()))

        val expectedApiContract = ApiContract.Recommendation.Get(RECOM_VARIANT_ID)
        val apiContractCaptured = slot<ApiContract>()
        every {
            apiClient.post(
                url = capture(apiContractCaptured),
                jsonBody = any(),
                responseHandler = any()
            )
        } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess(result.toJson())
        }

        // When
        SUT.getRecommendation(
            RECOM_VARIANT_ID,
            recomRequest,
            RecommendationResponseFull::class.java,
            responseCallback
        )

        // Then
        assertEquals(expectedApiContract.url, apiContractCaptured.captured.url)
        verify(exactly = 1) {
            apiClient.post(
                any(),
                eq(recomRequest.toRemote().toJson()),
                any()
            )
        }

        verify(exactly = 1) { responseCallback.onSuccess(eq(result)) }
        verify(exactly = 0) { responseCallback.onSuccessFallbackToJson(any()) }
        verify(exactly = 0) { responseCallback.onFailure(any(), any(), any()) }

        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
    }

    @Test
    fun givenServerFailure_whenGetRecommendations_thenOnFailureCalled() {
        // Given
        val recomRequest = RecomRequest(RECOM_PRODUCTS, RECOM_CATEGORY, RECOM_FIELDS)
        val responseCallback =
            mockk<GetRecommendationResponseCallback<RecommendationResponseFull>>(relaxed = true)

        val expectedApiContract = ApiContract.Recommendation.Get(RECOM_VARIANT_ID)
        val apiContractCaptured = slot<ApiContract>()
        every {
            apiClient.post(
                url = capture(apiContractCaptured),
                jsonBody = any(),
                responseHandler = any()
            )
        } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE_NON_REPEATABLE, ERROR_MSG, ERROR_EXCEPTION)
        }

        // When
        SUT.getRecommendation(
            RECOM_VARIANT_ID,
            recomRequest,
            RecommendationResponseFull::class.java,
            responseCallback
        )

        // Then
        assertEquals(expectedApiContract.url, apiContractCaptured.captured.url)
        verify(exactly = 1) {
            apiClient.post(
                any(),
                eq(recomRequest.toRemote().toJson()),
                any()
            )
        }

        verify(exactly = 0) { responseCallback.onSuccess(any()) }
        verify(exactly = 0) { responseCallback.onSuccessFallbackToJson(any()) }
        verify(exactly = 1) {
            responseCallback.onFailure(
                eq(ERROR_CODE_NON_REPEATABLE),
                eq(ERROR_MSG),
                eq(ERROR_EXCEPTION)
            )
        }

        verify(exactly = 1) { OperationQueue.addUiOperation(any()) }
    }

    @Test
    fun givenValidEvents_whenEventsSent_thenSaveEvents() {
        // Given
        val recomEvents = getRecomEvents()
        val recomEventsDb = getRecomEventsDb()

        // When
        SUT.saveRecommendations(recomEvents)

        // Then
        verify(exactly = 1) { OperationQueue.addOperation(any()) }
        verify(exactly = 1) { databaseManager.insertRecomEvents(recomEventsDb) }
    }

    @Test
    fun givenValidRecomEvents_whenRecomEventsPush_thenApiClientRecomEventsWithCorrectParameters() {
        // Given
        val recomEvents = getRecomEvents()
        val recomEventsDb = getRecomEventsDb()

        every { databaseManager.getRecomEvents(any()) } returns listOf(recomEventsDb) andThen emptyList()

        SUT.pushRecommendations()

        verify(exactly = 1) { apiClient.post(eq(ApiContract.Recommendation.Post), eq(listOf(recomEventsDb).toRemote().toJson()), any()) }
        verify(exactly = 0) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidRecomEvents_whenRecomEventsPushSuccessful_thenTryPushNextRecomEvents() {
        // Given
        val recomEvents = getRecomEvents()
        val recomEventsDb = getRecomEventsDb()
        every { databaseManager.getRecomEvents(any()) } returnsMany listOf(
            listOf(recomEventsDb),
            listOf(recomEventsDb),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onSuccess("")
        }

        // When
        SUT.pushRecommendations()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 2) { databaseManager.deleteRecomEvents(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenValidRecomEvents_whenRecomEventsPushFailedAndErrorIsRepeatable_cancelPushOperations() {
        val recomEventDb = getRecomEventsDb()
        every { databaseManager.getRecomEvents(any()) } returns listOf(recomEventDb)
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE_REPEATABLE, null, null)
        }

        SUT.pushRecommendations()

        verify(exactly = 1) { apiClient.post(any(), any(), any()) }
        verify(exactly = 1) { PushOperationQueue.removeAllOperations() }
    }

    @Test
    fun givenValidRecomEvents_whenRecomEventsPushFailedAndErrorIsNonRepeatable_thenTryPushNextRecomEvents() {
        // Given
        val recomEventsDb = getRecomEventsDb()
        every { databaseManager.getRecomEvents(any()) } returnsMany listOf(
            listOf(recomEventsDb),
            listOf(recomEventsDb),
            emptyList()
        )
        every { apiClient.post(url = any(), jsonBody = any(), responseHandler = any()) } answers {
            val callback = thirdArg<ResponseCallback>()
            callback.onFailure(ERROR_CODE_NON_REPEATABLE, null, null)
        }

        // When
        SUT.pushRecommendations()

        // Then
        verify(exactly = 2) { apiClient.post(any(), any(), any()) }
        verify(exactly = 3) { databaseManager.getRecomEvents() }
        verify(exactly = 2) { databaseManager.deleteRecomEvents(1) }
        verify(exactly = 1) { PushOperationQueue.nextOperation() }
    }

    @Test
    fun givenNoRecomEventsInDb_whenRecomEventsPush_thenApiClientIsNotCalled() {
        // Given
        every { databaseManager.getRecomEvents(any()) } returns emptyList()

        // When
        SUT.pushRecommendations()

        // Then
        verify(exactly = 0) { apiClient.post(any(), any(), any()) }
        verify { PushOperationQueue.nextOperation() }
    }

    @Test
    fun noOutdatedRecomEvent_whenClearOldRecommendations_thenSentNothing() {
        // Given
        every { databaseManager.deleteRecomEventsByTime(any()) } returns 0

        // When
        SUT.clearOldRecommendations(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManager.deleteRecomEventsByTime(any()) }
        verify(exactly = 0) { Logger.captureEvent(any()) }
    }

    @Test
    fun thereAreOutdatedInteraction_whenClearOldInteractions_thenSentCountDeleted() {
        // Given
        val deletedEvents = 2
        every { databaseManager.deleteRecomEventsByTime(any()) } returns deletedEvents
        val expectedMsg = "Outdated Events: - $deletedEvents"

        // When
        SUT.clearOldRecommendations(ZonedDateTime.now())

        // Then
        verify(exactly = 1) { databaseManager.deleteRecomEventsByTime(any()) }
        verify(exactly = 1) { Logger.captureEvent(eq(expectedMsg)) }
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getRecommendationResponseFull_1() = RecommendationResponseFull(
        productId = "24-WG081-blue",
        category = listOf("Fitness Equipment", "Default Category/Gear/Fitness Equipment"),
        categoryAncestor = listOf("Default Category"),
        categoryLayout = listOf("Fitness Equipment"),
        categoryParent = listOf("Gear", "Default Category/Gear"),
        date_created_as = "",
        date_created_es = "",
        date_modified_as = "",
        descr = "<p>The Sprite Stasis Ball gives you the toned abs, sides, and back you want by amping up your core workout. With bright colors and a burst-resistant design, it's a must-have for every hard-core exercise addict. Use for abdominal conditioning, balance training, yoga, or even physical therapy.</p>\\n<ul>\\n<li>Durable, burst-resistant design.</li>\\n<li>Hand pump included.</li>\\n</ul>",
        imageUrl = "https://recom.devel.ardas.dp.ua/media/catalog/product/l/u/luma-stability-ball.jpg",
        item_group = "",
        name = "generated-from-json Sprite Stasis Ball 55 cm",
        name_keyword = "",
        price = 23.0,
        product_id = "",
        tags_all_category_names = "Gear",
        tags_bestseller = "1",
        tags_cashback = "44",
        tags_category_bestseller = "false",
        tags_credit = "357",
        tags_delivery = "0",
        tags_description_price_range = "дорогой роскошный",
        tags_discount = "78",
        tags_has_purchases_21_days = "false",
        tags_is_bestseller = "false",
        tags_is_bestseller_by_categories = "false",
        tags_item_group_id = "24-WG081-blue",
        tags_num_purchases_21_days = "0",
        tags_old_price = "106",
        tags_oldprice = "106",
        tags_price_range = "high",
        tags_rating = "3",
        tags_sale = "0",
        url = "https://recom.devel.ardas.dp.ua/sprite%20stasis%20ball%2055%20cm%20blue.html?sc_content=24913_1"
    )

    private fun getRecommendationResponseFull_2() = RecommendationResponseFull(
        productId = "24-UG06",
        category = listOf("Fitness Equipment", "Default Category/Gear/Fitness Equipment"),
        categoryAncestor = listOf("Default Category"),
        categoryLayout = listOf("Fitness Equipment"),
        categoryParent = listOf("Gear", "Default Category/Gear"),
        date_created_as = "",
        date_created_es = "",
        date_modified_as = "",
        descr = "<p>You'll stay hydrated with ease with the Affirm Water Bottle by your side or in hand. Measurements on the outside help you keep track of how much you're drinking, while the screw-top lid prevents spills. A metal carabiner clip allows you to attach it to the outside of a backpack or bag for easy access.</p>\\n<ul>\\n<li>Made of plastic.</li>\\n<li>Grooved sides for an easy grip.</li>\\n</ul>\"",
        imageUrl = "https://recom.devel.ardas.dp.ua/media/catalog/product/u/g/ug06-lb-0.jpg",
        item_group = "",
        name = "generated-from-json Affirm Water Bottle",
        name_keyword = "",
        price = 7.0,
        product_id = "",
        tags_all_category_names = "Gear",
        tags_bestseller = "1",
        tags_cashback = "91",
        tags_category_bestseller = "false",
        tags_credit = "582",
        tags_delivery = "0",
        tags_description_price_range = "дешевый недорогой",
        tags_discount = "97",
        tags_has_purchases_21_days = "false",
        tags_is_bestseller = "false",
        tags_is_bestseller_by_categories = "false",
        tags_item_group_id = "24-UG06",
        tags_num_purchases_21_days = "0",
        tags_old_price = "213",
        tags_oldprice = "213",
        tags_price_range = "very_low",
        tags_rating = "4",
        tags_sale = "0",
        url = "https://recom.devel.ardas.dp.ua/affirm-water-bottle.html?sc_content=24913_1"
    )

    private fun getRecomEvents() = RecomEvents(
        recomVariantId = RECOM_VARIANT_ID,
        recomEvents = listOf(
            RecomEvent(
                recomEventType = RECOM_EVENT_TYPE,
                occurred = ZonedDateTime.now(),
                productId = PRODUCT_ID
            )
        )
    )

    private fun getRecomEventsDb() = RecomEventsDb(
        recomVariantId = RECOM_VARIANT_ID,
        recomEvents = listOf(
            RecomEventDb(
                recomEventType = RECOM_EVENT_TYPE_DB,
                occurred = ZonedDateTime.now().formatToRemote(),
                productId = PRODUCT_ID
            )
        )
    )
    // endregion helper methods --------------------------------------------------------------------


    // region helper classes -----------------------------------------------------------------------
    data class RecommendationResponseFull(
        override val productId: String,
        val category: List<String>?,
        val categoryAncestor: List<String>?,
        val categoryLayout: List<String>?,
        val categoryParent: List<String>?,
        val date_created_as: String?,
        val date_created_es: String?,
        val date_modified_as: String?,
        val descr: String?,
        val imageUrl: String?,
        val item_group: String?,
        val name: String?,
        val name_keyword: String?,
        val price: Double?,
        val product_id: String?,
        val tags_all_category_names: String?,
        val tags_bestseller: String?,
        val tags_cashback: String?,
        val tags_category_bestseller: String?,
        val tags_credit: String?,
        val tags_delivery: String?,
        val tags_description_price_range: String?,
        val tags_discount: String?,
        val tags_has_purchases_21_days: String?,
        val tags_is_bestseller: String?,
        val tags_is_bestseller_by_categories: String?,
        val tags_item_group_id: String?,
        val tags_num_purchases_21_days: String?,
        val tags_old_price: String?,
        val tags_oldprice: String?,
        val tags_price_range: String?,
        val tags_rating: String?,
        val tags_sale: String?,
        val url: String?
    ) : RecomBase
    // endregion helper classes --------------------------------------------------------------------
}