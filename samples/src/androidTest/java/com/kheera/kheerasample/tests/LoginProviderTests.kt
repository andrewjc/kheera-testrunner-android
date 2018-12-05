package com.kheera.kheerasample.tests

import androidx.test.runner.AndroidJUnit4
import com.kheera.kheerasample.auth.CredentialValidator

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LoginProviderTests {
    @Test
    fun testCredentialValidatorWorks() {
        val validator = CredentialValidator();
        assertTrue(validator.validate("admin@kheera.com", "admin123")== true)
    }
}
