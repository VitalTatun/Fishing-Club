package com.example.fishing.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fishing.data.AuthRepository
import com.example.fishing.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
}
