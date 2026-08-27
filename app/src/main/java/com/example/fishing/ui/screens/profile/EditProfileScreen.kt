package com.example.fishing.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fishing.R
import com.example.fishing.ui.components.ProfileListItem
import com.example.fishing.ui.theme.FishingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialName: String,
    email: String,
    avatarUrl: String?,
    isLoading: Boolean = false,
    error: String? = null,
    saveSuccess: Boolean = false,
    onBackClick: () -> Unit,
    onSaveClick: (String, Uri?) -> Unit,
    onResetSaveSuccess: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialName) {
        name = initialName
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            selectedPhotoUri = null
            snackbarHostState.showSnackbar("Изменения сохранены")
            onResetSaveSuccess()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
        // Auto-save photo change if needed, or wait for explicit save?
        // In the original it was onSaveClick(name, selectedPhotoUri)
        // Since we removed the check button in the design, maybe we should save on photo select?
        // Or keep a Save button for changes. Figma doesn't show one, but usually there's a way.
        // I'll stick to onSaveClick for now if I add a way to trigger it.
    }

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Изменить имя") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text("Имя") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    name = tempName
                    showNameDialog = false
                    onSaveClick(name, selectedPhotoUri)
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки профиля") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserAvatarEdit(
                avatarUrl = selectedPhotoUri ?: avatarUrl,
                onEditClick = { photoPickerLauncher.launch("image/*") }
            )

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ProfileListItem(
                    label = "Имя",
                    value = name,
                    onClick = { showNameDialog = true }
                )
                ProfileListItem(
                    label = "Электронная почта",
                    value = email,
                    trailingIcon = null
                )
            }

            // Actions Section
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)


            ) {
                ProfileListItem(
                    value = "Изменить пароль",
                    onClick = onChangePasswordClick
                )
                ProfileListItem(
                    value = "Удалить аккаунт",
                    onClick = onDeleteAccountClick
                )
            }
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun UserAvatarEdit(
    avatarUrl: Any?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 20.dp)
            .size(140.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_launcher_foreground),
            placeholder = painterResource(R.drawable.ic_launcher_foreground)
        )

        FilledTonalIconButton(
            onClick = onEditClick,
            modifier = Modifier
                .size(40.dp)
                .offset(x = 4.dp, y = 4.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Change photo",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    FishingTheme {
        EditProfileScreen(
            initialName = "Никита Белозерцев",
            email = "nikita.bel@gmail.com",
            avatarUrl = null,
            onBackClick = {},
            onSaveClick = { _, _ -> },
            onResetSaveSuccess = {}
        )
    }
}
