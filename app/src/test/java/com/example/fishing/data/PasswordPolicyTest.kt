package com.example.fishing.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AUD-03: the client checklist mirrors the Supabase password policy. */
class PasswordPolicyTest {

    @Test
    fun `ten characters with mixed case and a digit is accepted`() {
        val password = "Abcdefgh12"

        assertTrue(PasswordPolicy.isSatisfied(password))
        assertNull(PasswordPolicy.validate(password))
    }

    @Test
    fun `nine characters are rejected`() {
        val password = "Abcdefg12"

        assertFalse(PasswordPolicy.hasMinLength(password))
        assertEquals("Пароль должен быть минимум 10 символов", PasswordPolicy.validate(password))
    }

    @Test
    fun `missing lowercase is reported`() {
        assertEquals(
            "Пароль должен содержать строчную букву",
            PasswordPolicy.validate("ABCDEFGHIJ1")
        )
    }

    @Test
    fun `missing uppercase is reported`() {
        assertEquals(
            "Пароль должен содержать заглавную букву",
            PasswordPolicy.validate("abcdefghij1")
        )
    }

    @Test
    fun `missing digit is reported`() {
        assertEquals(
            "Пароль должен содержать цифру",
            PasswordPolicy.validate("Abcdefghij")
        )
    }

    @Test
    fun `cyrillic letters satisfy the letter requirements`() {
        val password = "Парольдлинный1"

        assertTrue(PasswordPolicy.hasLowercase(password))
        assertTrue(PasswordPolicy.hasUppercase(password))
        assertTrue(PasswordPolicy.hasDigit(password))
        assertTrue(PasswordPolicy.isSatisfied(password))
    }
}
