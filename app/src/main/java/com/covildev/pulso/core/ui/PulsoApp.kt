package com.covildev.pulso.core.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private const val PROFILE_EDIT_WARNING_MESSAGE =
    "Essas informa\u00e7\u00f5es devem ser preenchidas e acompanhadas por um profissional. Voc\u00ea tem certeza que quer editar?"

@Composable
fun PulsoApp(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    RequestNotificationPermissionEffect()

    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    if (profileUiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
            )
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
    onSaveProfile: suspend (String, String, String) -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var additionalInfo by rememberSaveable { mutableStateOf("") }
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
                additionalInfo = additionalInfo,
                onNameChange = { name = it },
                onAgeChange = { age = it },
                onAdditionalInfoChange = { additionalInfo = it },
                submitLabel = "Salvar perfil",
                onSubmit = {
                    coroutineScope.launch {
                        val saveResult = onSaveProfile(name, age, additionalInfo)
                        if (saveResult.isFailure) {
                            snackbarHostState.showSnackbar(
                                saveResult.exceptionOrNull()?.message
                                    ?: "Não foi possível salvar seu perfil.",
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
    onSaveProfile: suspend (String, String, String) -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    var showEditProfileScreen by rememberSaveable { mutableStateOf(false) }
    var showProfileEditWarning by rememberSaveable { mutableStateOf(false) }

    if (showEditProfileScreen) {
        EditProfileScreen(
            profile = profile,
            onBack = { showEditProfileScreen = false },
            onSaveProfile = onSaveProfile,
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AppBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        when (currentTab) {
            MainTab.DASHBOARD -> DashboardScreen(
                modifier = contentModifier,
                onProfileRequested = { showProfileEditWarning = true },
                onViewAllRequested = { currentTab = MainTab.REPORTS },
                onHelpRequested = {
                    val supportIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://covildev.com"),
                    )
                    runCatching { context.startActivity(supportIntent) }
                        .onFailure {
                            val message = if (it is ActivityNotFoundException) {
                                "Nenhum navegador encontrado neste dispositivo."
                            } else {
                                "Não foi possível abrir o site agora."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                },
            )

            MainTab.GOALS -> GoalsScreen(modifier = contentModifier)
            MainTab.REPORTS -> ReportsScreen(modifier = contentModifier)
        }
    }

    if (showProfileEditWarning) {
        AlertDialog(
            onDismissRequest = { showProfileEditWarning = false },
            title = { Text("Aviso") },
            text = { Text(PROFILE_EDIT_WARNING_MESSAGE) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProfileEditWarning = false
                        showEditProfileScreen = true
                    },
                ) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showProfileEditWarning = false },
                ) {
                    Text("Não")
                }
            },
        )
    }
}

@Composable
private fun AppBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                modifier = Modifier.padding(bottom = 5.dp),
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                ),
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
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditProfileScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onSaveProfile: suspend (String, String, String) -> Result<Unit>,
) {
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var age by rememberSaveable(profile.age) { mutableStateOf(profile.age.toString()) }
    var additionalInfo by rememberSaveable(profile.additionalInfo) { mutableStateOf(profile.additionalInfo) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "Atualize nome, idade e informações adicionais.",
            )
            ProfileForm(
                name = name,
                age = age,
                additionalInfo = additionalInfo,
                onNameChange = { name = it },
                onAgeChange = { age = it },
                onAdditionalInfoChange = { additionalInfo = it },
                submitLabel = "Salvar alteracoes",
                onSubmit = {
                    coroutineScope.launch {
                        val saveResult = onSaveProfile(name, age, additionalInfo)
                        if (saveResult.isSuccess) {
                            onBack()
                        } else {
                            snackbarHostState.showSnackbar(
                                saveResult.exceptionOrNull()?.message
                                    ?: "Não foi possível salvar seu perfil.",
                            )
                        }
                    }
                },
            )
        }
    }
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
    REPORTS("Relatórios", Icons.AutoMirrored.Filled.Assignment),
}
