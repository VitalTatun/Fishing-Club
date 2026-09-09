package com.example.fishing.data

object AuthErrorMapper {

    fun toUserFriendlyMessage(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ||
            msg.contains("Invalid email or password", ignoreCase = true) ->
                "Неверный email или пароль"

            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Email не подтверждён. Проверьте почту и перейдите по ссылке из письма"

            msg.contains("User already registered", ignoreCase = true) ->
                "Этот email уже зарегистрирован"

            msg.contains("Password should be at least", ignoreCase = true) ->
                "Пароль должен быть минимум 6 символов"

            msg.contains("rate limit", ignoreCase = true) ||
            msg.contains("429", ignoreCase = true) ->
                "Слишком много попыток. Попробуйте позже"

            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Network is unreachable", ignoreCase = true) ->
                "Нет соединения с интернетом"

            msg.contains("Email link is invalid or expired", ignoreCase = true) ->
                "Ссылка устарела"

            msg.contains("signup_disabled", ignoreCase = true) ->
                "Регистрация временно отключена"

            msg.contains("validation_error", ignoreCase = true) ->
                "Некорректные данные. Проверьте email и пароль"

            else -> e.message ?: "Неизвестная ошибка"
        }
    }
}