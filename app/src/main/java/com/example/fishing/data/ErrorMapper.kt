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

            msg.contains("Password should be at least", ignoreCase = true) ->
                "Пароль должен быть минимум 6 символов"

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

            else -> e.message?.takeIf { it.isNotBlank() } ?: "Произошла неизвестная ошибка"
        }
    }
}
