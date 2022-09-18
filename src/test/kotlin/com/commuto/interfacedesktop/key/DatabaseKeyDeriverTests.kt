package com.commuto.interfacedesktop.key

import org.junit.Test

/**
 * Tests for [DatabaseKeyDeriver].
 */
class DatabaseKeyDeriverTests {

    /**
     * Ensure [DatabaseKeyDeriver] consistently derives keys from a specific password and salt.
     */
    @Test
    fun testKeyDerivation() {
        val password = "an_example_password"
        val salt = "some_salt".toByteArray()

        val databaseKeyDeriver = DatabaseKeyDeriver(
            password = password,
            salt = salt,
        )

        val encryptedData = databaseKeyDeriver.key.encrypt("test".toByteArray())

        val anotherDatabaseKeyDeriver = DatabaseKeyDeriver(
            password = password,
            salt = salt,
        )

        assert("test".toByteArray().contentEquals(anotherDatabaseKeyDeriver.key.decrypt(encryptedData)))

    }

}