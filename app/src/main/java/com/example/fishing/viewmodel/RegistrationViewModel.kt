package com.example.fishing.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthErrorMapper
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegistrationResult {
    data object Idle : RegistrationResult
    data object Loading : RegistrationResult
    data object NeedsEmailConfirmation : RegistrationResult
    data object Success : RegistrationResult
    data class Error(val message: String) : RegistrationResult
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    private val _result = MutableStateFlow<RegistrationResult>(RegistrationResult.Idle)
    val result: StateFlow<RegistrationResult> = _result.asStateFlow()

    val isLoading: Boolean
        get() = _result.value is RegistrationResult.Loading

    fun register() {
        val validationError = validate()
        if (validationError != null) {
            _result.value = RegistrationResult.Error(validationError)
            return
        }

        viewModelScope.launch {
            _result.value = RegistrationResult.Loading
            val result = authRepository.register(email.trim(), password, name.trim())
            result.fold(
                onSuccess = { user ->
                    val hasSession = authRepository.currentUser() != null
                    _result.value = if (hasSession) {
                        RegistrationResult.Success
                    } else {
                        RegistrationResult.NeedsEmailConfirmation
                    }
                },
                onFailure = { e ->
                    _result.value = RegistrationResult.Error(
                        AuthErrorMapper.toUserFriendlyMessage(e)
                    )
                }
            )
        }
    }

    private fun validate(): String? {
        if (name.isBlank()) return "Введите имя"
        if (email.isBlank()) return "Введите email"
        if (!EmailValidator.isValid(email)) return "Некорректный формат email"
        if (password.isBlank()) return "Введите пароль"
        if (password.length < 6) return "Пароль должен быть минимум 6 символов"
        if (password != confirmPassword) return "Пароли не совпадают"
        return null
    }

    fun clearError() {
        if (_result.value is RegistrationResult.Error) {
            _result.value = RegistrationResult.Idle
        }
    }
}
