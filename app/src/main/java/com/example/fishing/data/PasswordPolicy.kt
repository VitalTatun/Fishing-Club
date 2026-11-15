package com.example.fishing.data

/**
 * AUD-03: password rules enforced by the Supabase Auth project
 * (Dashboard → Authentication → Sign In / Providers → Email).
 *
 * The server requires at least 10 characters with lowercase, uppercase and
 * digits, so the client checklist must match it: a password that passes here
 * can still be rejected by the server otherwise.
 */
object PasswordPolicy {

    const val MIN_LENGTH = 10

    fun hasMinLength(password: String): Boolean = password.length >= MIN_LENGTH

    fun hasLowercase(password: String): Boolean = password.any { it.isLowerCase() }

    fun hasUppercase(password: String): Boolean = password.any { it.isUpperCase() }

    fun hasDigit(password: String): Boolean = password.any { it.isDigit() }

    fun isSatisfied(password: String): Boolean =
        hasMinLength(password) &&
            hasLowercase(password) &&
            hasUppercase(password) &&
            hasDigit(password)

    /** First unmet requirement as a user-facing message, or `null` when valid. */
    fun validate(password: String): String? = when {
        !hasMinLength(password) -> "Пароль должен быть минимум $MIN_LENGTH символов"
        !hasLowercase(password) -> "Пароль должен содержать строчную букву"
        !hasUppercase(password) -> "Пароль должен содержать заглавную букву"
        !hasDigit(password) -> "Пароль должен содержать цифру"
        else -> null
    }
}
