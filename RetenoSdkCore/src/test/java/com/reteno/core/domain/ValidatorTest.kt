package com.reteno.core.domain

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class ValidatorTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private val SUBSCRIPTION_KEYS =
            listOf("subscriptionKey1", "subscriptionKey2", "subscriptionKey3")
        private val GROUP_NAMES_INCLUDE =
            listOf("groupNamesInclude1", "groupNamesInclude2", "groupNamesInclude3")
        private val GROUP_NAMES_EXCLUDE =
            listOf("groupNamesExclude1", "groupNamesExclude2", "groupNamesExclude3")

        private const val PHONE = "phone1"
        private const val EMAIL = "email1"
        private const val FIRST_NAME = "firstName1"
        private const val LAST_NAME = "lastName1"
        private const val LANGUAGE_CODE = "languageCode1"
        private const val TIME_ZONE = "timeZone1"
        private const val FIELD_KEY1 = "key1"
        private const val FIELD_KEY2 = "key2"
        private const val FIELD_KEY3 = "key3"
        private const val FIELD_VALUE1 = "value1"
        private const val FIELD_VALUE2 = "value2"

        private const val REGION = "region1"
        private const val TOWN = "town1"
        private const val ADDRESS = "address1"
        private const val POSTCODE = "postcode1"

        private val addressFull = Address(
            region = REGION,
            town = TOWN,
            address = ADDRESS,
            postcode = POSTCODE
        )

        private val customFieldsFull = listOf(
            UserCustomField(FIELD_KEY1, FIELD_VALUE1),
            UserCustomField(FIELD_KEY2, FIELD_VALUE2),
            UserCustomField(FIELD_KEY3, null)
        )
        private val userAttributesFull = UserAttributes(
            phone = PHONE,
            email = EMAIL,
            firstName = FIRST_NAME,
            lastName = LAST_NAME,
            languageCode = LANGUAGE_CODE,
            timeZone = TIME_ZONE,
            address = addressFull,
            fields = customFieldsFull
        )
        private val userFull = User(
            userAttributes = userAttributesFull,
            subscriptionKeys = SUBSCRIPTION_KEYS,
            groupNamesInclude = GROUP_NAMES_INCLUDE,
            groupNamesExclude = GROUP_NAMES_EXCLUDE,
        )
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    private var SUT = Validator
    // endregion helper fields ---------------------------------------------------------------------

    @Test
    fun givenAllAddressFieldsNull_whenValidateUser_thenAddressIsNull() {
        // Given
        val userAddress = Address(null, null, null, null)
        val userAttributes = userAttributesFull.copy(address = userAddress)
        val user = userFull.copy(userAttributes = userAttributes)

        val expectedAttributes = userAttributes.copy(address = null)
        val expectedUser = userFull.copy(userAttributes = expectedAttributes)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(expectedUser, userValidated)
    }

    @Test
    fun givenOneAddressFieldNull_whenValidateUser_thenUserUnchanged() {
        // Given
        val userAddress = addressFull.copy(region = null)
        val userAttributes = userAttributesFull.copy(address = userAddress)
        val user = userFull.copy(userAttributes = userAttributes)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(user, userValidated)
    }

    @Test
    fun givenOneAddressFieldNonNull_whenValidateUser_thenUserUnchanged() {
        // Given
        val userAddress = Address(region = null, town = null, address = null, postcode = POSTCODE)
        val userAttributes = userAttributesFull.copy(address = userAddress)
        val user = userFull.copy(userAttributes = userAttributes)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(user, userValidated)
    }

    @Test
    fun givenCustomFieldsNull_whenValidateUser_thenUserUnchanged() {
        // Given
        val userAttributes = userAttributesFull.copy(fields = null)
        val user = userFull.copy(userAttributes = userAttributes)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(user, userValidated)
    }

    @Test
    fun givenAddressNull_whenValidateUser_thenUserUnchanged() {
        // Given
        val userAttributes = userAttributesFull.copy(address = null)
        val user = userFull.copy(userAttributes = userAttributes)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(user, userValidated)
    }

    @Test
    fun givenOneUserAttributeNonNull_whenValidateUser_thenUserUnchanged() {
        // Given
        val userAttributes = UserAttributes(
            phone = PHONE,
            email = null,
            firstName = null,
            lastName = null,
            languageCode = null,
            timeZone = null,
            address = null,
            fields = null
        )
        val user = userFull.copy(userAttributes = userAttributes)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(user, userValidated)
    }

    @Test
    fun givenAllAddressFieldsNullAllAttributesNull_whenValidateUser_thenAttributesIsNull() {
        // Given
        val userAddress = Address(null, null, null, null)
        val userAttributes = UserAttributes(
            phone = null,
            email = null,
            firstName = null,
            lastName = null,
            languageCode = null,
            timeZone = null,
            address = userAddress,
            fields = null
        )
        val user = userFull.copy(userAttributes = userAttributes)

        val expectedUser = userFull.copy(userAttributes = null)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(expectedUser, userValidated)
    }

    @Test
    fun givenAllAttributesNull_whenValidateUser_thenAttributesIsNull() {
        // Given
        val userAttributes = UserAttributes(
            phone = null,
            email = null,
            firstName = null,
            lastName = null,
            languageCode = null,
            timeZone = null,
            address = null,
            fields = null
        )
        val user = userFull.copy(userAttributes = userAttributes)

        val expectedUser = userFull.copy(userAttributes = null)

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertEquals(expectedUser, userValidated)
    }

    @Test
    fun givenAllUserFieldsNull_whenValidateUser_thenAttributesIsNull() {
        // Given
        val user = User(
            userAttributes = null,
            subscriptionKeys = null,
            groupNamesInclude = null,
            groupNamesExclude = null,
        )

        // When
        val userValidated = SUT.validateUser(user)

        // Then
        assertNull(userValidated)
    }
}