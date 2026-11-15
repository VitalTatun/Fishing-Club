package com.example.fishing.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fishing.data.PasswordPolicy
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.ChangePasswordViewModel

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onBackClick: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val success by viewModel.success.collectAsStateWithLifecycle()

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isCurrentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isNewPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(success) {
        if (success) {
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
            isCurrentPasswordVisible = false
            isNewPasswordVisible = false
            isConfirmPasswordVisible = false
            snackbarHostState.showSnackbar("Пароль изменён")
            viewModel.consumeSuccess()
        }
    }

    ChangePasswordContent(
        currentPassword = currentPassword,
        newPassword = newPassword,
        confirmPassword = confirmPassword,
        isCurrentPasswordVisible = isCurrentPasswordVisible,
        isNewPasswordVisible = isNewPasswordVisible,
        isConfirmPasswordVisible = isConfirmPasswordVisible,
        isLoading = isLoading,
        error = error,
        snackbarHostState = snackbarHostState,
        onCurrentPasswordChange = {
            currentPassword = it
            viewModel.clearError()
        },
        onNewPasswordChange = {
            newPassword = it
            viewModel.clearError()
        },
        onConfirmPasswordChange = {
            confirmPassword = it
            viewModel.clearError()
        },
        onToggleCurrentPasswordVisibility = {
            isCurrentPasswordVisible = !isCurrentPasswordVisible
        },
        onToggleNewPasswordVisibility = { isNewPasswordVisible = !isNewPasswordVisible },
        onToggleConfirmPasswordVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
        onBackClick = onBackClick,
        onSubmit = { viewModel.changePassword(currentPassword, newPassword, confirmPassword) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordContent(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    isCurrentPasswordVisible: Boolean,
    isNewPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    isLoading: Boolean,
    error: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleCurrentPasswordVisibility: () -> Unit,
    onToggleNewPasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val canSubmit = !isLoading &&
        currentPassword.isNotBlank() &&
        newPassword.isNotBlank() &&
        newPassword == confirmPassword &&
        newPassword != currentPassword &&
        PasswordPolicy.isSatisfied(newPassword)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Изменить пароль") },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onSubmit,
                        enabled = canSubmit || isLoading,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current
                            )
                        } else {
                            Text("Сохранить")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                isError = error != null,
                label = { Text("Текущий пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = passwordTransformation(isCurrentPasswordVisible),
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = isCurrentPasswordVisible,
                        enabled = !isLoading,
                        onClick = onToggleCurrentPasswordVisibility
                    )
                }
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                isError = error != null,
                label = { Text("Новый пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = passwordTransformation(isNewPasswordVisible),
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = isNewPasswordVisible,
                        enabled = !isLoading,
                        onClick = onToggleNewPasswordVisibility
                    )
                }
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                isError = error != null,
                label = { Text("Повторите пароль") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = passwordTransformation(isConfirmPasswordVisible),
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isVisible = isConfirmPasswordVisible,
                        enabled = !isLoading,
                        onClick = onToggleConfirmPasswordVisibility
                    )
                }
            )

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Пароль должен содержать:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PasswordRequirementRow(
                    text = "Минимум ${PasswordPolicy.MIN_LENGTH} символов",
                    isMet = PasswordPolicy.hasMinLength(newPassword)
                )
                PasswordRequirementRow(
                    text = "Минимум одну строчную букву",
                    isMet = PasswordPolicy.hasLowercase(newPassword)
                )
                PasswordRequirementRow(
                    text = "Минимум одну заглавную букву",
                    isMet = PasswordPolicy.hasUppercase(newPassword)
                )
                PasswordRequirementRow(
                    text = "Минимум одну цифру",
                    isMet = PasswordPolicy.hasDigit(newPassword)
                )
            }
        }
    }
}

@Composable
private fun passwordTransformation(isVisible: Boolean): VisualTransformation =
    if (isVisible) VisualTransformation.None else PasswordVisualTransformation()

@Composable
private fun PasswordVisibilityToggle(
    isVisible: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (isVisible) "Скрыть пароль" else "Показать пароль"
        )
    }
}

@Composable
private fun PasswordRequirementRow(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .semantics {
                    contentDescription = if (isMet) "Выполнено" else "Не выполнено"
                },
            contentAlignment = Alignment.Center
        ) {
            if (isMet) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = FishingTheme.colors.success,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                )
            }
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePasswordContentPreview() {
    FishingTheme {
        ChangePasswordContent(
            currentPassword = "Qwerty1234",
            newPassword = "Abcdefgh12",
            confirmPassword = "Abcdefgh12",
            isCurrentPasswordVisible = false,
            isNewPasswordVisible = false,
            isConfirmPasswordVisible = false,
            isLoading = false,
            error = null,
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onToggleCurrentPasswordVisibility = {},
            onToggleNewPasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onBackClick = {},
            onSubmit = {}
        )
    }
}
