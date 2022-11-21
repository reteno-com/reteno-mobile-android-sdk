package com.reteno.core.domain

import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.util.allElementsNull

object Validator {

    fun validateUser(user: User): User? {
        val userAttrsValidated = validateAttributes(user.userAttributes)

        if (allElementsNull(
                user.subscriptionKeys,
                user.groupNamesInclude,
                user.groupNamesExclude,
                userAttrsValidated
            )
        ) {
            return null
        }

        return user.copy(userAttributes = userAttrsValidated)
    }

    private fun validateAttributes(userAttributes: UserAttributes?): UserAttributes? {
        val addressValidated = validateAddress(userAttributes?.address)

        val userAttrsValidated = if (userAttributes == null || allElementsNull(
                userAttributes.phone,
                userAttributes.email,
                userAttributes.firstName,
                userAttributes.lastName,
                userAttributes.languageCode,
                userAttributes.timeZone,
                userAttributes.fields,
                addressValidated
            )
        ) {
            null
        } else {
            userAttributes.copy(address = addressValidated)
        }
        return userAttrsValidated
    }

    private fun validateAddress(address: Address?): Address? =
        if (address == null || allElementsNull(
                address.region,
                address.town,
                address.address,
                address.postcode
            )
        ) {
            null
        } else {
            address
        }
}