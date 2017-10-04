package com.kheera.kheerasample.auth

class CredentialValidator {
    fun validate(mEmail: String, mPassword: String): Boolean? {
        return DUMMY_CREDENTIALS
                .map { it.split(":") }
                .firstOrNull { it[0] == mEmail }
                ?.let {
                    // Account exists, return true if the password matches.
                    it[1] == mPassword
                }
                ?: false
    }

    companion object {
        /**
         * A dummy authentication store containing known user names and passwords.
         * TODO: remove after connecting to a real authentication system.
         */
        private val DUMMY_CREDENTIALS = arrayOf("admin@kheera.com:admin123", "andrewc@kheera.com:password123")
    }
}