package com.example.fishing.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** AUD-03: server password errors are translated with the real threshold. */
class ErrorMapperTest {

    @Test
    fun `password length error keeps the number from the server`() {
        assertEquals(
            "Пароль должен быть минимум 10 символов",
            ErrorMapper.toUserFriendlyMessage(
                Exception("Password should be at least 10 characters.")
            )
        )
    }

    @Test
    fun `password length error follows a changed server policy`() {
        assertEquals(
            "Пароль должен быть минимум 8 символов",
            ErrorMapper.toUserFriendlyMessage(
                Exception("Password should be at least 8 characters.")
            )
        )
    }

    @Test
    fun `password length error falls back to the policy default`() {
        assertEquals(
            "Пароль должен быть минимум 10 символов",
            ErrorMapper.toUserFriendlyMessage(
                Exception("Password should be at least characters")
            )
        )
    }

    @Test
    fun `character requirements error is translated`() {
        assertEquals(
            "Пароль должен содержать строчную и заглавную буквы и цифру",
            ErrorMapper.toUserFriendlyMessage(
                Exception(
                    "Password should contain at least one character of each: " +
                        "abcdefghijklmnopqrstuvwxyz, ABCDEFGHIJKLMNOPQRSTUVWXYZ, 0123456789."
                )
            )
        )
    }

    @Test
    fun `wrong current password is translated`() {
        // AuthRestException appends the request details to the message.
        val message = """
            current_password_mismatch
            Current password required when setting new password.: current_password_mismatch
            URL: https://example.supabase.co/auth/v1/user
            Headers: [Authorization: Bearer ***]
            Http Method: PUT
        """.trimIndent()

        assertEquals(
            "Неверный текущий пароль",
            ErrorMapper.toUserFriendlyMessage(Exception(message))
        )
    }

    @Test
    fun `missing current password is translated`() {
        assertEquals(
            "Введите текущий пароль",
            ErrorMapper.toUserFriendlyMessage(
                Exception(
                    "current_password_required\n" +
                        "Current password required when setting new password.: " +
                        "current_password_required"
                )
            )
        )
    }

    @Test
    fun `same password error is translated`() {
        assertEquals(
            "Новый пароль должен отличаться от текущего",
            ErrorMapper.toUserFriendlyMessage(
                Exception(
                    "same_password\n" +
                        "New password should be different from the old password.: same_password"
                )
            )
        )
    }

    @Test
    fun `reauthentication error is translated`() {
        assertEquals(
            "Сессия слишком старая для смены пароля. Войдите в аккаунт заново",
            ErrorMapper.toUserFriendlyMessage(
                Exception(
                    "reauthentication_needed\n" +
                        "Password update requires reauthentication: reauthentication_needed"
                )
            )
        )
    }

    @Test
    fun `unmapped server error does not leak request details`() {
        val message = """
            unexpected_failure
            Something broke: unexpected_failure
            URL: https://example.supabase.co/auth/v1/user
            Headers: [Authorization: Bearer ***]
            Http Method: PUT
        """.trimIndent()

        assertEquals(
            "Не удалось выполнить действие. Попробуйте позже",
            ErrorMapper.toUserFriendlyMessage(Exception(message))
        )
    }

    @Test
    fun `leaked password error is translated`() {
        assertEquals(
            "Пароль слишком простой. Придумайте другой",
            ErrorMapper.toUserFriendlyMessage(
                Exception("Password is known to be weak and easy to guess, please pick a different one.")
            )
        )
    }
}
