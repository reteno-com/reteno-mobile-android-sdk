package com.reteno.core.internal.action.user

import java.util.Locale

internal object UserAttributesValidator {

    private const val MAX_MARKET_ID_LENGTH = 64
    private const val ISO_639_1_LENGTH = 2
    private val marketIdSymbols = setOf('-', '_')

    fun requireValidExternalUserId(externalUserId: String) {
        require(externalUserId.isNotBlank()) { "externalUserId should not be null or blank" }
    }

    fun requireValidMarketId(marketId: String?) {
        if (marketId == null) return
        val allowedSymbols = marketId.all { it.isLetterOrDigit() || it in marketIdSymbols }
        require(allowedSymbols && marketId.length <= MAX_MARKET_ID_LENGTH) {
            "Invalid marketId, max length is 64, allowed symbols: latin characters, digits, '-', '_'"
        }
    }

    fun languageCodeWithRegion(languageCode: String): String? {
        val locale = Locale.forLanguageTag(languageCode)
        val tag = locale.toLanguageTag()
        val hasLanguageAndRegion = locale.language.length == ISO_639_1_LENGTH && locale.country.isNotEmpty()
        val wellFormed = hasLanguageAndRegion && tag.equals(languageCode, ignoreCase = true)
        return if (wellFormed) locale.stripExtensions().toLanguageTag() else null
    }
}
