package com.reteno.core.domain

import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.util.allElementsNull

object Validator {

    fun validateUser(user: User): User? {
        if (allElementsNull(
                user.subscriptionKeys,
                user.groupNamesInclude,
                user.groupNamesExclude
            )
        ) {
            return null
        }

        val userAttrsValidated = validateAttributes(user.userAttributes)
        return user.copy(userAttributes = userAttrsValidated)
    }

    private fun validateAttributes(userAttributes: UserAttributes?): UserAttributes? {
        val userAttrsValidated = if (userAttributes == null || allElementsNull(
                userAttributes.phone,
                userAttributes.email,
                userAttributes.firstName,
                userAttributes.lastName,
                userAttributes.languageCode,
                userAttributes.timeZone,
                userAttributes.address,
                userAttributes.fields
            )
        ) {
            null
        } else {
            val addressValidated = validateAddress(userAttributes.address)
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