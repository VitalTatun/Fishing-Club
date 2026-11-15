package com.example.fishing.data

object ErrorMapper {

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

            // Both current-password errors share the same server message and
            // differ only by code, so the code has to be checked first.
            msg.contains("current_password_mismatch", ignoreCase = true) ->
                "Неверный текущий пароль"

            msg.contains("current_password_required", ignoreCase = true) ||
            msg.contains("Current password required", ignoreCase = true) ->
                "Введите текущий пароль"

            msg.contains("should be different from the old password", ignoreCase = true) ||
            msg.contains("same_password", ignoreCase = true) ->
                "Новый пароль должен отличаться от текущего"

            msg.contains("requires reauthentication", ignoreCase = true) ||
            msg.contains("reauthentication_needed", ignoreCase = true) ->
                "Сессия слишком старая для смены пароля. Войдите в аккаунт заново"

            msg.contains("should contain at least one character", ignoreCase = true) ->
                "Пароль должен содержать строчную и заглавную буквы и цифру"

            // The server owns the threshold: read the actual number from its
            // message instead of assuming a fixed minimum (AUD-03).
            msg.contains("Password should be at least", ignoreCase = true) ->
                "Пароль должен быть минимум ${minLengthFrom(msg)} символов"

            msg.contains("known to be weak", ignoreCase = true) ||
            msg.contains("easy to guess", ignoreCase = true) ->
                "Пароль слишком простой. Придумайте другой"

            msg.contains("rate limit", ignoreCase = true) ||
            msg.contains("429", ignoreCase = true) ->
                "Слишком много попыток. Попробуйте позже"

            msg.contains("network error", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("SocketTimeoutException", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Network is unreachable", ignoreCase = true) ||
            msg.contains("UnknownHostException", ignoreCase = true) ||
            msg.contains("No address associated with hostname", ignoreCase = true) ||
            msg.contains("http request", ignoreCase = true) ||
            msg.contains("ktor", ignoreCase = true) ||
            msg.contains("clientrequestexception", ignoreCase = true) ||
            msg.contains("serverresponseexception", ignoreCase = true) ->
                "Нет соединения с интернетом"

            msg.contains("Email link is invalid or expired", ignoreCase = true) ->
                "Ссылка устарела"

            msg.contains("Photo file not found", ignoreCase = true) ->
                "Фото было удалено, выберите его заново"

            msg.contains("Auth session missing", ignoreCase = true) ||
            msg.contains("session_not_found", ignoreCase = true) ->
                "Сессия истекла. Войдите в аккаунт ещё раз"

            msg.contains("Not authenticated", ignoreCase = true) ->
                "Сессия истекла. Войдите в аккаунт ещё раз"

            msg.contains("signup_disabled", ignoreCase = true) ->
                "Регистрация временно отключена"

            msg.contains("validation_error", ignoreCase = true) ->
                "Некорректные данные. Проверьте введённые данные"

            msg.contains("500", ignoreCase = true) ||
            msg.contains("502", ignoreCase = true) ||
            msg.contains("503", ignoreCase = true) ||
            msg.contains("504", ignoreCase = true) ->
                "Ошибка сервера. Попробуйте позже"

            msg.contains("401", ignoreCase = true) ||
            msg.contains("403", ignoreCase = true) ||
            msg.contains("Unauthorized", ignoreCase = true) ->
                "Сессия истекла. Войдите в аккаунт ещё раз"

            msg.contains("404", ignoreCase = true) ||
            msg.contains("Not found", ignoreCase = true) ->
                "Запрашиваемый ресурс не найден"

            else -> fallbackMessage(msg)
        }
    }

    /**
     * RestException appends the request URL, headers and HTTP method to the
     * message. Those details must never reach the UI, so an unmapped server
     * error is replaced with a short generic text.
     */
    private fun fallbackMessage(message: String): String =
        when {
            message.isBlank() -> "Произошла неизвестная ошибка"
            message.contains("URL:") || message.contains("Http Method:") ->
                "Не удалось выполнить действие. Попробуйте позже"
            else -> message
        }

    private val MIN_LENGTH_PATTERN = Regex("""at least (\d+)""", RegexOption.IGNORE_CASE)

    private fun minLengthFrom(message: String): Int =
        MIN_LENGTH_PATTERN.find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: PasswordPolicy.MIN_LENGTH
}
