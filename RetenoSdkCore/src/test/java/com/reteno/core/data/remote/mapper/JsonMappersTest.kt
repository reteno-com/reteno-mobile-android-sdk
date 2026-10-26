package com.reteno.core.data.remote.mapper

import com.reteno.core.data.remote.model.recommendation.get.RecomBase
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class JsonMappersTest {

    private data class Product(override val productId: String, val name: String?) : RecomBase

    @Test
    fun `should read nothing from blank json`() = runUnitTest {
        given()
        val json = "  "

        whenn()
        val action = json.fromJsonOrNull<InteractionAction>()

        then()
        assertNull(action)
    }

    @Test
    fun `should read object from json`() = runUnitTest {
        given()
        val json = InteractionAction("CLICK", "btn", "u").toJson()

        whenn()
        val read = listOf(json.fromJsonOrNull<InteractionAction>(), json.fromJson(InteractionAction::class.java))

        then()
        assertEquals(listOf(InteractionAction("CLICK", "btn", "u"), InteractionAction("CLICK", "btn", "u")), read)
    }

    @Test
    fun `should read list from json`() = runUnitTest {
        given()
        val json = """["a","b"]"""

        whenn()
        val list = json.listFromJson<String>()

        then()
        assertEquals(listOf("a", "b"), list)
    }

    @Test
    fun `should read every recommendation of a response as the host model`() = runUnitTest {
        given()
        val json = """{"recoms":[{"productId":"sku-1","name":"Shoe"},{"productId":"sku-2"}]}"""

        whenn()
        val recoms = json.convertRecoms(Product::class.java)

        then()
        assertEquals(listOf(Product("sku-1", "Shoe"), Product("sku-2", null)), recoms.recoms)
    }
}
