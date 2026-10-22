package com.example

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.preferences.UserSettings
import com.example.di.AppContainer
import com.example.domain.model.AppThemeMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AppLockGateScreen
import com.example.ui.screens.CameraAndEdgeScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.EnhanceAndOcrScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PdfToolsAndConvertScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DocuScanTheme
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.ui.viewmodel.ScannerCaptureMode
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
object CameraScanRoute

@Serializable
object EnhanceOcrRoute

@Serializable
object PdfToolsRoute

@Serializable
object SettingsRoute

@Serializable
data class DocumentDetailRoute(val documentId: String)

class MainActivity : FragmentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer = AppContainer(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            val currentUser by remember { FirebaseAuth.getInstance().authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = FirebaseAuth.getInstance().currentUser)
            val rootSettings by appContainer.settingsRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = UserSettings())

            DocuScanTheme(themeMode = rootSettings.themeMode) {
                val isDark = LocalIsDarkGlass.current
                if (currentUser == null && !rootSettings.isGuestMode) {
                    AuthScreen(
                        onContinueAsGuest = {
                            scope.launch {
                                appContainer.settingsRepository.setGuestMode(true)
                            }
                        },
                        onAuthSuccess = {
                            scope.launch {
                                appContainer.settingsRepository.setGuestMode(false)
                            }
                        },
                        onToggleTheme = {
                            scope.launch {
                                appContainer.settingsRepository.setThemeMode(
                                    if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                )
                            }
                        }
                    )
                } else {
                    val activeUserId = currentUser?.uid ?: "guest"
                    val viewModel: DocuScanViewModel = viewModel(
                        key = "docuscan_vm_$activeUserId",
                        factory = DocuScanViewModel.provideFactory(
                            appContainer = appContainer,
                            appContext = applicationContext,
                            currentUserId = activeUserId
                        )
                    )
                    val settings by viewModel.settings.collectAsStateWithLifecycle()
                    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

                    if (settings.biometricLockEnabled && !isAppUnlocked) {
                        AppLockGateScreen(
                            expectedPin = settings.appLockPin,
                            onUnlocked = { viewModel.markAppUnlocked() }
                        )
                    } else {
                        DocuScanMainShell(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DocuScanMainShell(viewModel: DocuScanViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkGlass.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3500L)
            viewModel.clearStatus()
        }
    }

    val multiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 25)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.startNewScanSession(ScannerCaptureMode.STANDARD_BATCH)
            viewModel.onMultipleGalleryUrisSelected(
                uris = uris,
                onReadyForCrop = {
                    navController.navigate(CameraScanRoute)
                },
                onReadyForEnhance = {
                    navController.navigate(EnhanceOcrRoute)
                }
            )
        }
    }

    val showBottomBar = currentDestination?.let { dest ->
        dest.hasRoute<LibraryRoute>() ||
            dest.hasRoute<PdfToolsRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: true

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = if (isDark) {
                        Color(0xFF0B1528).copy(alpha = 0.84f)
                    } else {
                        Color.White.copy(alpha = 0.80f)
                    },
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute<LibraryRoute>() == true,
                        onClick = {
                            navController.navigate(LibraryRoute) {
                                popUpTo(LibraryRoute) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Library") },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("nav_tab_library")
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute<CameraScanRoute>() == true,
                        onClick = {
                            viewModel.startNewScanSession(ScannerCaptureMode.STANDARD_BATCH)
                            navController.navigate(CameraScanRoute) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.DocumentScanner, contentDescription = "Scan") },
                        label = { Text("Scan") },
                        modifier = Modifier.testTag("nav_tab_scan")
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute<PdfToolsRoute>() == true,
                        onClick = {
                            navController.navigate(PdfToolsRoute) {
                                popUpTo(LibraryRoute) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                        label = { Text("Tools") },
                        modifier = Modifier.testTag("nav_tab_pdf_tools")
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute<SettingsRoute>() == true,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(LibraryRoute) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = LibraryRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<LibraryRoute> {
                    LibraryScreen(
                        viewModel = viewModel,
                        onLaunchScanner = { mode ->
                            viewModel.startNewScanSession(mode)
                            navController.navigate(CameraScanRoute)
                        },
                        onPickGalleryImages = {
                            multiGalleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onOpenDocument = { docId ->
                            navController.navigate(DocumentDetailRoute(docId))
                        }
                    )
                }

                composable<CameraScanRoute> {
                    CameraAndEdgeScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onProceedToEnhance = {
                            navController.navigate(EnhanceOcrRoute)
                        }
                    )
                }

                composable<EnhanceOcrRoute> {
                    EnhanceAndOcrScreen(
                        viewModel = viewModel,
                        onAddMorePages = {
                            navController.navigate(CameraScanRoute)
                        },
                        onSavedDocument = { savedDocId ->
                            navController.navigate(DocumentDetailRoute(savedDocId)) {
                                popUpTo(LibraryRoute) { inclusive = false }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<PdfToolsRoute> {
                    PdfToolsAndConvertScreen(
                        viewModel = viewModel,
                        onOpenDocument = { docId ->
                            navController.navigate(DocumentDetailRoute(docId))
                        },
                        onStartSpecializedScan = { mode ->
                            viewModel.startNewScanSession(mode)
                            navController.navigate(CameraScanRoute)
                        },
                        onNavigateToCrop = {
                            navController.navigate(CameraScanRoute)
                        },
                        onNavigateToEnhance = {
                            navController.navigate(EnhanceOcrRoute)
                        }
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    viewModel.setGuestMode(false)
                                },
                                scope = scope
                            )
                        },
                        onSwitchToWelcomeScreen = {
                            viewModel.setGuestMode(false)
                        }
                    )
                }

                composable<DocumentDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<DocumentDetailRoute>()
                    DocumentDetailScreen(
                        documentId = route.documentId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Floating Glass Status Pill
            AnimatedVisibility(
                visible = statusBanner != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                FrostedGlassCard(
                    cornerRadius = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.clearStatus() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
