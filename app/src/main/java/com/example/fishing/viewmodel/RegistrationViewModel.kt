package com.example.fishing.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    fun register() {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _error.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.register(email.trim(), password, name.trim())
            result.fold(
                onSuccess = {
                    _isRegistered.value = true
                },
                onFailure = { e ->
                    _error.value = userFriendlyError(e)
                }
            )
            _isLoading.value = false
        }
    }

    private fun userFriendlyError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
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
            else -> e.message ?: "Неизвестная ошибка"
        }
    }

    fun clearError() {
        _error.value = null
    }
}
