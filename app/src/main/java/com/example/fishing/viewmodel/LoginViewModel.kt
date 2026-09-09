package com.example.fishing.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthErrorMapper
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.EmailValidator
import com.example.fishing.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login() {
        val validationError = validate()
        if (validationError != null) {
            _error.value = validationError
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.login(email.trim(), password)
            result.fold(
                onSuccess = {
                    _error.value = null
                },
                onFailure = { e ->
                    _error.value = AuthErrorMapper.toUserFriendlyMessage(e)
                }
            )
            _isLoading.value = false
        }
    }

    private fun validate(): String? {
        if (email.isBlank()) return "Введите email"
        if (!EmailValidator.isValid(email)) return "Некорректный формат email"
        if (password.isBlank()) return "Введите пароль"
        return null
    }

    fun clearError() {
        _error.value = null
    }
}
