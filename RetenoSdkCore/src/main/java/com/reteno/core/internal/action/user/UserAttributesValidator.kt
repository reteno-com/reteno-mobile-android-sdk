package com.reteno.core.internal.action.user

internal object UserAttributesValidator {

    private const val MAX_MARKET_ID_LENGTH = 64
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
}
