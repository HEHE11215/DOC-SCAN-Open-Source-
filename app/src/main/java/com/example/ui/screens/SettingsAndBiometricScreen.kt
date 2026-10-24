package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppThemeMode
import com.example.domain.model.OcrScript
import com.example.domain.model.PdfPageSize
import com.example.domain.model.PdfQuality
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.launchSignInWithGoogleSheet
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.GlassThemeTogglePill
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.viewmodel.DocuScanViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: DocuScanViewModel,
    onSignOut: () -> Unit,
    onSwitchToWelcomeScreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkGlass.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authInstance = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }
    }
    val currentUser by remember(authInstance) {
        authInstance?.authStateFlow() ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = authInstance?.currentUser)
    var showPinDialog by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    GlassScreenBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Clean iOS Glass Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    GlassThemeTogglePill(
                        isDark = isDark,
                        onToggle = {
                            viewModel.setThemeMode(
                                if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 2-Mode Glass Theme Card (Light Mode vs Dark Mode)
                    SettingsCard(
                        title = "Glass Theme",
                        icon = Icons.Default.Palette
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassActionButton(
                                text = "Light Glass",
                                icon = Icons.Default.LightMode,
                                isPrimary = !isDark,
                                onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_light")
                            )
                            GlassActionButton(
                                text = "Dark Glass",
                                icon = Icons.Default.DarkMode,
                                isPrimary = isDark,
                                onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_dark")
                            )
                        }
                    }

                    // Account & Guest Mode Card
                    SettingsCard(
                        title = if (currentUser != null) "Google Account" else "Guest Mode",
                        icon = if (currentUser != null) Icons.Default.CloudSync else Icons.Default.PersonOutline
                    ) {
                        if (currentUser != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cloud Backup",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentUser?.email ?: currentUser?.displayName ?: "Signed In",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.autoCloudSync,
                                    onCheckedChange = { viewModel.setAutoCloudSync(it) },
                                    modifier = Modifier.testTag("settings_cloud_sync_switch")
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassActionButton(
                                    text = "Sync",
                                    icon = Icons.Default.CloudSync,
                                    isPrimary = true,
                                    onClick = { viewModel.syncLibraryWithCloud() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_sync_now_button")
                                )
                                GlassActionButton(
                                    text = "Sign Out",
                                    icon = Icons.AutoMirrored.Filled.Logout,
                                    isPrimary = false,
                                    onClick = onSignOut,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_sign_out_button")
                                )
                            }
                        } else {
                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassActionButton(
                                    text = "Google Login",
                                    icon = Icons.AutoMirrored.Filled.Login,
                                    isPrimary = true,
                                    onClick = {
                                        authError = null
                                        launchSignInWithGoogleSheet(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                viewModel.setGuestMode(false)
                                            },
                                            onAuthError = { msg -> authError = msg },
                                            scope = scope
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_google_login_button")
                                )
                                GlassActionButton(
                                    text = "Welcome",
                                    isPrimary = false,
                                    onClick = onSwitchToWelcomeScreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_welcome_screen_button")
                                )
                            }
                        }
                    }

                    // Default PDF Export Settings
                    SettingsCard(
                        title = "PDF Defaults",
                        icon = Icons.Default.PictureAsPdf
                    ) {
                        Text("Page Size", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PdfPageSize.entries.forEach { size ->
                                FilterChip(
                                    selected = settings.defaultPageSize == size,
                                    onClick = { viewModel.setDefaultPageSize(size) },
                                    label = { Text(size.label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Quality", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PdfQuality.entries.forEach { quality ->
                                FilterChip(
                                    selected = settings.defaultPdfQuality == quality,
                                    onClick = { viewModel.setDefaultPdfQuality(quality) },
                                    label = { Text(quality.label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Searchable OCR Layer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = settings.defaultSearchablePdf,
                                onCheckedChange = { viewModel.setDefaultSearchablePdf(it) },
                                modifier = Modifier.testTag("settings_searchable_pdf_switch")
                            )
                        }
                    }

                    // Default OCR Script
                    SettingsCard(
                        title = "OCR Language",
                        icon = Icons.Default.Translate
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OcrScript.entries.forEach { script ->
                                FilterChip(
                                    selected = settings.defaultOcrScript == script,
                                    onClick = { viewModel.setDefaultOcrScript(script) },
                                    label = { Text(script.label) },
                                    modifier = Modifier.testTag("settings_ocr_${script.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Biometric App Lock Section
                    SettingsCard(
                        title = "App Lock",
                        icon = Icons.Default.Fingerprint
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Biometric / PIN Lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = settings.biometricLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.setBiometricLock(false, "")
                                    }
                                },
                                modifier = Modifier.testTag("settings_biometric_lock_switch")
                            )
                        }
                    }

                    // Release & GitHub Build Info Card
                    SettingsCard(
                        title = "Doc Scan Release",
                        icon = Icons.Default.CloudSync
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Version",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "v1.0.0 (Build 1 • Release Ready)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "GitHub Actions CI/CD is configured in .github/workflows/build-apk.yml to automatically build & publish Doc-Scan-v1.0.0-release.apk and Doc-Scan-v1.0.0-debug.apk to GitHub Releases on every push.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(108.dp))
                }
            }
        }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("1234") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Backup PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.take(8) },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPinDialog = false
                        viewModel.setBiometricLock(true, pinInput.ifBlank { "1234" })
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    FrostedGlassCard(
        cornerRadius = 22.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Full-screen Biometric & PIN Gate shown when App Lock is enabled in Settings.
 */
@Composable
fun AppLockGateScreen(
    expectedPin: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pinEntry by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun triggerBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onUnlocked()
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Doc Scan")
                .setSubtitle("Authenticate with Biometrics or PIN")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            prompt.authenticate(info)
        } else {
            errorText = "Enter Backup PIN below."
        }
    }

    LaunchedEffect(Unit) {
        triggerBiometricPrompt()
    }

    GlassScreenBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            FrostedGlassCard(
                cornerRadius = 26.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Doc Scan Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = pinEntry,
                        onValueChange = {
                            pinEntry = it
                            errorText = null
                        },
                        label = { Text("Backup PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_lock_pin_input")
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassActionButton(
                            text = "Biometrics",
                            icon = Icons.Default.Fingerprint,
                            isPrimary = false,
                            onClick = { triggerBiometricPrompt() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("app_lock_biometric_button")
                        )
                        GlassActionButton(
                            text = "Unlock",
                            isPrimary = true,
                            onClick = {
                                if (pinEntry == expectedPin || expectedPin.isBlank()) {
                                    onUnlocked()
                                } else {
                                    errorText = "Incorrect PIN"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("app_lock_unlock_button")
                        )
                    }
                }
            }
        }
    }
}
