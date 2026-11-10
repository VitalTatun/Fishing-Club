package com.example.fishing.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.IoDispatcher
import com.example.fishing.model.User
import com.example.fishing.utils.PhotoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(authRepository.currentUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError: StateFlow<String?> = _deleteAccountError.asStateFlow()

    fun updateProfile(
        contentResolver: ContentResolver,
        filesDir: File,
        name: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var localImagePath: String? = null

            try {
                // Decoding, EXIF handling, scaling and JPEG re-encoding are heavy;
                // doing them on Main froze the UI and caused an ANR. The state is
                // set before suspending, so the spinner keeps drawing meanwhile.
                localImagePath = imageUri?.let { uri ->
                    withContext(ioDispatcher) {
                        PhotoUtils.copyPhotoToInternalStorage(contentResolver, filesDir, uri)
                    }
                }

                val result = authRepository.updateProfile(name, localImagePath)
                if (result.isSuccess) {
                    _user.value = result.getOrNull()
                    _saveSuccess.value = true
                } else {
                    _error.value = result.exceptionOrNull()?.let { ErrorMapper.toUserFriendlyMessage(it) } ?: "Неизвестная ошибка"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorMapper.toUserFriendlyMessage(e)
            } finally {
                // The avatar is uploaded to Storage; the local temp copy is no
                // longer needed, so filesDir/photos does not accumulate files.
                // Cleanup must still run when the scope is cancelled.
                localImagePath?.let { path ->
                    withContext(ioDispatcher + NonCancellable) {
                        runCatching { File(path).delete() }
                    }
                }
                _isLoading.value = false
            }
        }
    }
    
    fun resetSuccess() {
        _saveSuccess.value = false
    }

    fun deleteAccount() {
        if (_isDeletingAccount.value) return

        _deleteAccountError.value = null
        viewModelScope.launch {
            _isDeletingAccount.value = true
            try {
                authRepository.deleteAccount()
                    .onFailure { e ->
                        _deleteAccountError.value = ErrorMapper.toUserFriendlyMessage(e)
                    }
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    fun clearDeleteAccountError() {
        _deleteAccountError.value = null
    }
}
