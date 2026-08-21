package com.example.fishing.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.ui.screens.report.create.CreateReportColors
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel,
    onBackClick: () -> Unit,
    onRegistered: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()

    LaunchedEffect(isRegistered) {
        if (isRegistered) {
            onRegistered()
        }
    }

    RegistrationContent(
        name = viewModel.name,
        email = viewModel.email,
        password = viewModel.password,
        isLoading = isLoading,
        error = error,
        onNameChange = { viewModel.name = it },
        onEmailChange = { viewModel.email = it },
        onPasswordChange = { viewModel.password = it },
        onRegisterClick = { viewModel.register() },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationContent(
    name: String,
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Регистрация") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreateReportColors.ScreenBackground,
                    titleContentColor = CreateReportColors.OnSurface,
                    navigationIconContentColor = CreateReportColors.OnSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CreateReportColors.ScreenBackground)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                supportingText = {
                    if (error != null) {
                        Text(text = error)
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRegisterClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreateReportColors.OnSurface
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = CreateReportColors.Surface,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Text(
                        text = "Зарегистрироваться",
                        style = MaterialTheme.typography.labelLarge,
                        color = CreateReportColors.Surface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistrationScreenPreview() {
    FishingTheme {
        RegistrationContent(
            name = "Иван",
            email = "test@example.com",
            password = "password",
            isLoading = false,
            error = null,
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onRegisterClick = {},
            onBackClick = {}
        )
    }
}
