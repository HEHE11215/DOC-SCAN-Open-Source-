package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.LiquidGlassBottomBar
import com.example.ui.theme.LiquidGlassNavTab
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.ui.viewmodel.ScannerCaptureMode
import com.example.utils.LocalHaptics
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
    val haptics = LocalHaptics.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val scanSession by viewModel.scanSession.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    // 4-Tab HorizontalPager state: 0 = Library, 1 = Scan, 2 = Tools, 3 = Settings
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    var lastHapticPage by rememberSaveable { mutableIntStateOf(0) }

    // Trigger subtle tactile vibration haptic when swiping left/right settles on a new tab
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != lastHapticPage) {
            lastHapticPage = pagerState.settledPage
            haptics.tick()
        }
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3500L)
            viewModel.clearStatus()
        }
    }

    fun switchToPagerTab(targetPage: Int) {
        scope.launch {
            if (currentDestination?.hasRoute<LibraryRoute>() != true) {
                navController.popBackStack(LibraryRoute, inclusive = false)
            }
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
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
                    switchToPagerTab(1)
                },
                onReadyForEnhance = {
                    navController.navigate(EnhanceOcrRoute)
                }
            )
        }
    }

    val isOnMainPager = currentDestination?.let { dest ->
        dest.hasRoute<LibraryRoute>() ||
            dest.hasRoute<CameraScanRoute>() ||
            dest.hasRoute<PdfToolsRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: true

    val showBottomBar = isOnMainPager &&
        !(pagerState.currentPage == 1 && scanSession.isCroppingStage)

    val activeTabIndex = when {
        currentDestination?.hasRoute<CameraScanRoute>() == true -> 1
        currentDestination?.hasRoute<PdfToolsRoute>() == true -> 2
        currentDestination?.hasRoute<SettingsRoute>() == true -> 3
        else -> pagerState.currentPage
    }

    val continuousTabProgress = if (currentDestination?.hasRoute<LibraryRoute>() == true) {
        (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 3f)
    } else {
        activeTabIndex.toFloat()
    }

    // Root full-bleed GlassScreenBackground so content and floating glass nav bar blend 100% seamlessly
    GlassScreenBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = LibraryRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<LibraryRoute> {
                    // Back handler when on Scan, Tools, or Settings tab returns smoothly to Library tab
                    BackHandler(enabled = pagerState.currentPage != 0 && !scanSession.isCroppingStage) {
                        haptics.tick()
                        switchToPagerTab(0)
                    }

                    HorizontalPager(
                        state = pagerState,
                        // Disable horizontal page swipe while dragging 4-corner perspective crop handles
                        userScrollEnabled = !(pagerState.currentPage == 1 && scanSession.isCroppingStage),
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> LibraryScreen(
                                viewModel = viewModel,
                                onLaunchScanner = { mode ->
                                    viewModel.startNewScanSession(mode)
                                    switchToPagerTab(1)
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

                            1 -> CameraAndEdgeScreen(
                                viewModel = viewModel,
                                isActiveTab = pagerState.currentPage == 1 || pagerState.targetPage == 1,
                                showBottomNavClearance = !scanSession.isCroppingStage,
                                onBack = {
                                    switchToPagerTab(0)
                                },
                                onProceedToEnhance = {
                                    navController.navigate(EnhanceOcrRoute)
                                }
                            )

                            2 -> PdfToolsAndConvertScreen(
                                viewModel = viewModel,
                                onOpenDocument = { docId ->
                                    navController.navigate(DocumentDetailRoute(docId))
                                },
                                onStartSpecializedScan = { mode ->
                                    viewModel.startNewScanSession(mode)
                                    switchToPagerTab(1)
                                },
                                onNavigateToCrop = {
                                    switchToPagerTab(1)
                                },
                                onNavigateToEnhance = {
                                    navController.navigate(EnhanceOcrRoute)
                                }
                            )

                            3 -> SettingsScreen(
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
                    }
                }

                composable<CameraScanRoute> {
                    CameraAndEdgeScreen(
                        viewModel = viewModel,
                        isActiveTab = true,
                        showBottomNavClearance = !scanSession.isCroppingStage,
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
                            navController.popBackStack()
                            switchToPagerTab(1)
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
                            switchToPagerTab(1)
                        },
                        onNavigateToCrop = {
                            switchToPagerTab(1)
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

            // Floating Glass Status Pill positioned cleanly above the Liquid Glass Bottom Bar
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (showBottomBar) 84.dp else 14.dp
                    )
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
                        IconButton(
                            onClick = {
                                haptics.click()
                                viewModel.clearStatus()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Seamless Floating Liquid Glass Navigation Bar (overlays content for zero-seam Light/Dark blending)
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LiquidGlassBottomBar(
                    selectedProgress = continuousTabProgress,
                    tabs = listOf(
                        LiquidGlassNavTab(
                            label = "Library",
                            icon = Icons.Default.Folder,
                            selected = activeTabIndex == 0,
                            testTag = "nav_tab_library",
                            onClick = { switchToPagerTab(0) }
                        ),
                        LiquidGlassNavTab(
                            label = "Scan",
                            icon = Icons.Default.DocumentScanner,
                            selected = activeTabIndex == 1,
                            testTag = "nav_tab_scan",
                            onClick = {
                                if (activeTabIndex != 1) {
                                    viewModel.startNewScanSession(ScannerCaptureMode.STANDARD_BATCH)
                                }
                                switchToPagerTab(1)
                            }
                        ),
                        LiquidGlassNavTab(
                            label = "Tools",
                            icon = Icons.Default.Build,
                            selected = activeTabIndex == 2,
                            testTag = "nav_tab_pdf_tools",
                            onClick = { switchToPagerTab(2) }
                        ),
                        LiquidGlassNavTab(
                            label = "Settings",
                            icon = Icons.Default.Settings,
                            selected = activeTabIndex == 3,
                            testTag = "nav_tab_settings",
                            onClick = { switchToPagerTab(3) }
                        )
                    )
                )
            }
        }
    }
}
