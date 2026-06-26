package com.example.fishing.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: com.example.fishing.viewmodel.LoginViewModel,
    onAuthenticated: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val email = viewModel.email
    val password = viewModel.password

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onAuthenticated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreateReportColors.ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Fishing Journal",
            style = MaterialTheme.typography.headlineMedium,
            color = CreateReportColors.OnSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(4.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CreateReportColors.Surface,
                unfocusedContainerColor = CreateReportColors.Surface,
                disabledContainerColor = CreateReportColors.Surface,
                focusedBorderColor = CreateReportColors.Outline,
                unfocusedBorderColor = CreateReportColors.Outline,
                focusedLabelColor = CreateReportColors.OnSurface,
                unfocusedLabelColor = CreateReportColors.OnSurfaceVariant,
                focusedTextColor = CreateReportColors.OnSurface,
                unfocusedTextColor = CreateReportColors.OnSurface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.password = it },
            label = { Text("Пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(4.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CreateReportColors.Surface,
                unfocusedContainerColor = CreateReportColors.Surface,
                disabledContainerColor = CreateReportColors.Surface,
                focusedBorderColor = CreateReportColors.Outline,
                unfocusedBorderColor = CreateReportColors.Outline,
                focusedLabelColor = CreateReportColors.OnSurface,
                unfocusedLabelColor = CreateReportColors.OnSurfaceVariant,
                focusedTextColor = CreateReportColors.OnSurface,
                unfocusedTextColor = CreateReportColors.OnSurface
            )
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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
                    text = "Войти",
                    style = MaterialTheme.typography.labelLarge,
                    color = CreateReportColors.Surface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.register() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CreateReportColors.OnSurface
            )
        ) {
            Text(
                text = "Создать аккаунт",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
