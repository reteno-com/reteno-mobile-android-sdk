package com.reteno.core.internal.action.iam

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.file.FileManager
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.mapper.fromJson
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutParams.Position
import com.reteno.core.data.remote.model.iam.message.InAppMessageContent.InAppLayoutType
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubInAppContent
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubUserDb
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LoadInAppHtmlActionTest {

    private class SutFixture(scope: TestScope, users: List<UserDb> = emptyList()) {
        val files = mockk<FileManager>()
        val usersDb = mockk<RetenoDatabaseManagerUser>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var usersReadOnIo = false
        var baseHtmlReadOnIo = false
        val sut = LoadInAppHtmlAction(
            files = files,
            usersDb = usersDb,
            dispatchers = dispatchers,
            newId = { "html-1" },
            runner = traced.runner
        )

        init {
            coEvery { files.getBaseHtmlContent() } answers {
                baseHtmlReadOnIo = dispatchers.markedIo.isRunningHere
                BASE_HTML
            }
            every { usersDb.getUsers(any()) } answers {
                usersReadOnIo = dispatchers.markedIo.isRunningHere
                users
            }
        }

        suspend fun personalise(text: String): String {
            val content = stubInAppContent(model = JsonObject().apply { addProperty("text", text) })
            val html = sut(stubInAppMessage(content = content)).getOrThrow().fullHtml
            val model = html.removePrefix("<html>").substringBefore("|")
            return JsonParser.parseString(model).asJsonObject.get("text").asString
        }
    }

    @Test
    fun `should put message model into base html with empty personalisation`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val content = stubInAppContent(model = JsonObject().apply { addProperty("text", "hi") })

        whenn()
        val result = fixture.sut(stubInAppMessage(content = content)).getOrThrow()

        then()
        assertEquals("""<html>{"text":"hi"}|{}</html>""", result.fullHtml)
    }

    @Test
    fun `should keep layout of message content`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val content = InAppMessageContent(
            messageInstanceId = 1,
            layoutType = InAppLayoutType.SLIDE_UP,
            layoutParams = InAppLayoutParams(Position.BOTTOM),
            model = JsonObject()
        )

        whenn()
        val result = fixture.sut(stubInAppMessage(content = content)).getOrThrow()

        then()
        assertEquals(InAppLayoutType.SLIDE_UP, result.layoutType)
        assertEquals(InAppLayoutParams(Position.BOTTOM), result.layoutParams)
    }

    @Test
    fun `should place message at top when content has no layout params`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(stubInAppMessage()).getOrThrow()

        then()
        assertEquals(InAppLayoutParams(Position.TOP), result.layoutParams)
    }

    @Test
    fun `should show content of unknown layout type full screen`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val json = """{"messageInstanceId":1,"layoutType":"CAROUSEL","model":{"text":"hi"}}"""
        val content = json.fromJson<InAppMessageContent>()

        whenn()
        val result = fixture.sut(stubInAppMessage(content = content)).getOrThrow()

        then()
        assertEquals(InAppLayoutType.FULL, result.layoutType)
    }

    @Test
    fun `should put empty model into base html for content without model`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val content = """{"messageInstanceId":1,"layoutType":"FULL"}""".fromJson<InAppMessageContent>()

        whenn()
        val result = fixture.sut(stubInAppMessage(content = content)).getOrThrow()

        then()
        assertEquals("<html>{}|{}</html>", result.fullHtml)
    }

    @Test
    fun `should give each html a new id`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(stubInAppMessage()).getOrThrow()

        then()
        assertEquals("html-1", result.id)
    }

    @Test
    fun `should fail for message without content`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(stubInAppMessage(content = null))

        then()
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should build html on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubInAppMessage())

        then()
        assertTrue(fixture.baseHtmlReadOnIo)
    }

    @Test
    fun `should not read users when model has no placeholders`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.personalise("Hello")

        then()
        verify(exactly = 0) { fixture.usersDb.getUsers(any()) }
    }

    @Test
    fun `should read users on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this, users = listOf(pendingUser(firstName = "Ann")))

        whenn()
        fixture.personalise("Hello \${firstName}")

        then()
        assertTrue(fixture.usersReadOnIo)
    }

    @Test
    fun `should prefer newest pending user over synced user`() = runUnitTest {
        given()
        val fixture = SutFixture(
            this,
            users = listOf(
                stubUserDb(firstName = "Synced", synced = BooleanDb.TRUE),
                pendingUser(firstName = "Older", createdAt = 1),
                pendingUser(firstName = "Newest", createdAt = 2)
            )
        )

        whenn()
        val text = fixture.personalise("Hello \${firstName}")

        then()
        assertEquals("Hello Newest", text)
    }

    @Test
    fun `should take attribute missing from pending user from synced user`() = runUnitTest {
        given()
        val fixture = SutFixture(
            this,
            users = listOf(
                stubUserDb(firstName = "Synced", synced = BooleanDb.TRUE),
                pendingUser(email = "new@example.com")
            )
        )

        whenn()
        val text = fixture.personalise("\${firstName} \${email}")

        then()
        assertEquals("Synced new@example.com", text)
    }

    @Test
    fun `should leave placeholder empty without users`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val text = fixture.personalise("Hello \${firstName}!")

        then()
        assertEquals("Hello !", text)
    }

    @Test
    fun `should fill every supported placeholder`() = runUnitTest {
        given()
        val attributes = UserAttributesDb(
            phone = "+380501234567",
            email = "ann@example.com",
            firstName = "Ann",
            lastName = "Lee",
            languageCode = "uk",
            timeZone = "Europe/Kyiv",
            address = AddressDb(address = "Main st 1"),
            marketId = "m-1"
        )
        val synced = UserDb(deviceId = "d", userAttributes = attributes, isSynchronizedWithBackend = BooleanDb.TRUE)
        val fixture = SutFixture(this, users = listOf(synced))

        whenn()
        val text = fixture.personalise(
            "\${firstName};\${lastName};\${email};\${phone};\${languageCode};\${timeZone};\${address};\${marketId}"
        )

        then()
        assertEquals("Ann;Lee;ann@example.com;+380501234567;uk;Europe/Kyiv;Main st 1;m-1", text)
    }

    @Test
    fun `should leave address empty when user has no address`() = runUnitTest {
        given()
        val fixture = SutFixture(this, users = listOf(pendingUser(firstName = "Ann")))

        whenn()
        val text = fixture.personalise("[\${address}]")

        then()
        assertEquals("[]", text)
    }

    @Test
    fun `should keep quotes in user value as literal text`() = runUnitTest {
        given()
        val fixture = SutFixture(this, users = listOf(pendingUser(firstName = "Ann\", \"admin\": \"true")))

        whenn()
        val text = fixture.personalise("Hello \${firstName}")

        then()
        assertEquals("Hello Ann\", \"admin\": \"true", text)
    }

    @Test
    fun `should keep script tags in user value as literal text`() = runUnitTest {
        given()
        val fixture = SutFixture(this, users = listOf(pendingUser(firstName = "</script><script>alert(1)</script>")))

        whenn()
        val html = fixture.sut(
            stubInAppMessage(content = stubInAppContent(JsonObject().apply { addProperty("text", "\${firstName}") }))
        ).getOrThrow().fullHtml

        then()
        assertTrue("</script>" !in html)
        assertTrue("\\u003c/script\\u003e" in html)
    }

    @Test
    fun `should keep unicode in user value`() = runUnitTest {
        given()
        val fixture = SutFixture(this, users = listOf(pendingUser(firstName = "Олена 😀")))

        whenn()
        val text = fixture.personalise("Hi \${firstName}")

        then()
        assertEquals("Hi Олена 😀", text)
    }

    @Test
    fun `should not trace user values`() = runUnitTest {
        given()
        val fixture = SutFixture(this, users = listOf(pendingUser(firstName = "Ann", email = "ann@example.com")))

        whenn()
        fixture.personalise("\${firstName} \${email}")

        then()
        assertTrue(fixture.traced.sink.lines.none { it.contains("Ann") || it.contains("ann@example.com") })
    }

    private fun pendingUser(firstName: String? = null, email: String? = null, createdAt: Long = 0) =
        stubUserDb(firstName = firstName, email = email, createdAt = createdAt, synced = BooleanDb.FALSE)

    private companion object {
        const val BASE_HTML = "<html>\${documentModel}|\${personalisation}</html>"
    }
}
