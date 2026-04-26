package com.covildev.pulso.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.covildev.pulso.feature_metas.ui.GoalsScreen
import com.covildev.pulso.feature_perfil.domain.model.UserProfile
import com.covildev.pulso.feature_perfil.ui.ProfileForm
import com.covildev.pulso.feature_perfil.ui.ProfileViewModel
import com.covildev.pulso.feature_registro.ui.DashboardScreen
import com.covildev.pulso.feature_relatorio.ui.ReportsScreen
import kotlinx.coroutines.launch

@Composable
fun PulsoApp(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    RequestNotificationPermissionEffect()

    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    if (profileUiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val profile = profileUiState.profile
    if (profile == null) {
        OnboardingScreen(
            onSaveProfile = profileViewModel::saveProfile,
            modifier = modifier,
        )
    } else {
        MainAppScaffold(
            profile = profile,
            onSaveProfile = profileViewModel::saveProfile,
            modifier = modifier,
        )
    }
}

@Composable
private fun OnboardingScreen(
    onSaveProfile: suspend (String, String) -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Bem-vindo ao Pulso")
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "Antes de comecar, informe seus dados de perfil.",
            )
            ProfileForm(
                name = name,
                age = age,
                onNameChange = { name = it },
                onAgeChange = { age = it },
                submitLabel = "Salvar perfil",
                onSubmit = {
                    coroutineScope.launch {
                        val saveResult = onSaveProfile(name, age)
                        if (saveResult.isFailure) {
                            snackbarHostState.showSnackbar(
                                saveResult.exceptionOrNull()?.message
                                    ?: "Nao foi possivel salvar seu perfil.",
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun MainAppScaffold(
    profile: UserProfile,
    onSaveProfile: suspend (String, String) -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        when (currentTab) {
            MainTab.DASHBOARD -> DashboardScreen(
                modifier = contentModifier,
                onProfileRequested = { showProfileDialog = true },
            )

            MainTab.GOALS -> GoalsScreen(modifier = contentModifier)
            MainTab.REPORTS -> ReportsScreen(modifier = contentModifier)
        }
    }

    if (showProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = onSaveProfile,
        )
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: suspend (String, String) -> Result<Unit>,
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var age by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var localError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = age,
                    onValueChange = { age = it.filter(Char::isDigit) },
                    label = { Text("Idade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                localError?.let {
                    Text(it)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        val result = onSaveProfile(name, age)
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            localError = result.exceptionOrNull()?.message ?: "Erro ao salvar."
                        }
                    }
                },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun RequestNotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {}

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private enum class MainTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    DASHBOARD("Principal", Icons.Default.Home),
    GOALS("Metas", Icons.Default.NotificationsActive),
    REPORTS("Relatorios", Icons.AutoMirrored.Filled.Assignment),
}
