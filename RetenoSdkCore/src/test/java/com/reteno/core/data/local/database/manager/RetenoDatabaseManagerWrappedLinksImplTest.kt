package com.reteno.core.data.local.database.manager

import com.reteno.core.fixtures.TestDatabase
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoDatabaseManagerWrappedLinksImplTest {

    private class SutFixture(val db: TestDatabase) {
        val sut = RetenoDatabaseManagerWrappedLinksImpl(db.database)

        fun store(vararg urls: String) {
            urls.forEachIndexed { index, url ->
                sut.insertWrappedLink(url)
                db.execute("UPDATE WrappedLink SET timeStamp = ? WHERE url = ?", "2026-01-0${index + 1} 00:00:00", url)
            }
        }
    }

    private suspend fun withSut(block: suspend SutFixture.() -> Unit) = TestDatabase.open { SutFixture(this).block() }

    @Test
    fun `should read links oldest first up to the limit`() = runUnitTest {
        withSut {
            given()
            store("https://a.example/1", "https://a.example/2")

            whenn()
            val links = listOf(sut.getWrappedLinks(), sut.getWrappedLinks(limit = 1))

            then()
            val expected = listOf(listOf("https://a.example/1", "https://a.example/2"), listOf("https://a.example/1"))
            assertEquals(expected, links)
            assertEquals(2L, sut.getWrappedLinksCount())
        }
    }

    @Test
    fun `should remove a stored link without url`() = runUnitTest {
        withSut {
            given()
            db.execute("INSERT INTO WrappedLink (url) VALUES (NULL)")

            whenn()
            val links = sut.getWrappedLinks()

            then()
            assertEquals(emptyList<String>(), links)
            assertEquals(0L, sut.getWrappedLinksCount())
        }
    }

    @Test
    fun `should delete the oldest or the newest links`() = runUnitTest {
        withSut {
            given()
            store("https://a.example/1", "https://a.example/2", "https://a.example/3", "https://a.example/4")

            whenn()
            sut.deleteWrappedLinks(count = 1)
            sut.deleteWrappedLinks(count = 1, oldest = false)

            then()
            assertEquals(listOf("https://a.example/2", "https://a.example/3"), sut.getWrappedLinks())
        }
    }

    @Test
    fun `should keep links stored later on the same day as the cutoff`() = runUnitTest {
        withSut {
            given()
            sut.insertWrappedLink("https://a.example/1")
            db.execute("UPDATE WrappedLink SET timeStamp = '2026-01-02 10:00:00'")

            whenn()
            val deleted = sut.deleteWrappedLinksByTime("2026-01-02T09:00:00Z")

            then()
            assertEquals(0, deleted)
        }
    }

    @Test
    fun `should delete links stored before a time`() = runUnitTest {
        withSut {
            given()
            store("https://a.example/1", "https://a.example/2")

            whenn()
            val deleted = sut.deleteWrappedLinksByTime("2026-01-02T00:00:00Z")

            then()
            assertEquals(1, deleted)
            assertTrue(sut.getWrappedLinks() == listOf("https://a.example/2"))
        }
    }
}
