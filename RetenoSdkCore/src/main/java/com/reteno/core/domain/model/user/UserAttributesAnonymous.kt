package com.reteno.core.domain.model.user

data class UserAttributesAnonymous(

    /**
     *  Contact first name. Numbers cannot be used
     */
    val firstName: String? = null,

    /**
     * Contact last name. Numbers cannot be used
     */
    val lastName: String? = null,

    /**
     * Data about language in RFC 5646 format.
     * Primary language subtag in ISO 639-1 format is required.
     * Example: de-AT
     */
    val languageCode: String? = null,

    /**
     * Item from TZ database. Example: Europe/Kyiv.
     * See column [TZ database name](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
     */
    val timeZone: String? = null,

    /**
     * Contact full address
     *
     * @see Address
     */
    val address: Address? = null,

    /**
     * Additional field values for contact
     *
     * @see UserCustomField
     */
    val fields: List<UserCustomField>? = null,
    /**
     * Market ID
     *
     * Max length: 64 char
     * Allowed symbols: latin symbols, numbers and `-`, `_`
     * To be able to clear market id, pass empty string, null values are ignored
     */
    val marketId: String? = null,
) {

    fun toUserAttributes(): UserAttributes =
        UserAttributes(
            phone = null,
            email = null,
            firstName = firstName,
            lastName = lastName,
            languageCode = languageCode,
            timeZone = timeZone,
            address = address,
            fields = fields,
            marketId = marketId
        )
}