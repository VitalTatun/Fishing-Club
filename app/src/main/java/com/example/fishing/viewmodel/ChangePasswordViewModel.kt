package com.example.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.PasswordPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** AUD-03: change the password of the signed-in account. */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (_isLoading.value) return

        _error.value = null
        _success.value = false

        if (currentPassword.isBlank()) {
            _error.value = "Введите текущий пароль"
            return
        }
        PasswordPolicy.validate(newPassword)?.let { validationError ->
            _error.value = validationError
            return
        }
        if (newPassword != confirmPassword) {
            _error.value = "Пароли не совпадают"
            return
        }
        if (newPassword == currentPassword) {
            _error.value = "Новый пароль должен отличаться от текущего"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.changePassword(newPassword, currentPassword)
                    .onSuccess { _success.value = true }
                    .onFailure { e ->
                        _error.value = ErrorMapper.toUserFriendlyMessage(e)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun consumeSuccess() {
        _success.value = false
    }
}
