package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.GlassThemeTogglePill
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.theme.RoyalBlue
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        onAuthSuccess()
        return
    }

    val activityContext = context as? Activity ?: run {
        onUnauthenticated()
        return
    }
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId == 0) {
        onUnauthenticated()
        return
    }
    val webClientId = context.getString(resId)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun launchSignInWithGoogleSheet(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val activityContext = context as? Activity ?: run {
        onAuthError("Sign-in requires an active Activity context.")
        return
    }
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId == 0) {
        onAuthError("Missing default_web_client_id in google-services.json.")
        return
    }
    val webClientId = context.getString(resId)

    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("Auth", "User cancelled the sign-in dialog.")
        } catch (e: Exception) {
            onAuthError(e.localizedMessage ?: "Sign-in failed. Continue as Guest anytime.")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    FirebaseAuth.getInstance().signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthScreen(
    onContinueAsGuest: () -> Unit,
    onAuthSuccess: () -> Unit,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkGlass.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    GlassScreenBackground(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top-right 1-tap Light / Dark Glass Mode toggle
        GlassThemeTogglePill(
            isDark = isDark,
            onToggle = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .testTag("auth_theme_toggle")
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 440.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FrostedGlassCard(
                cornerRadius = 30.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // iOS Glass Emblem
                    FrostedGlassCard(
                        cornerRadius = 24.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(44.dp)) {
                                val w = size.width
                                val h = size.height
                                drawRoundRect(
                                    color = RoyalBlue.copy(alpha = 0.16f),
                                    topLeft = Offset(w * 0.12f, h * 0.08f),
                                    size = Size(w * 0.76f, h * 0.84f),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                drawRoundRect(
                                    color = RoyalBlue,
                                    topLeft = Offset(w * 0.12f, h * 0.08f),
                                    size = Size(w * 0.76f, h * 0.84f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                    style = Stroke(width = 4.5f)
                                )
                                drawLine(
                                    color = ElectricTeal,
                                    start = Offset(0f, h * 0.50f),
                                    end = Offset(w, h * 0.50f),
                                    strokeWidth = 6f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Doc Scan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "FREE Direct PDF Editor • B&W Scanner • Share Pictures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = ElectricTeal.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "★ FREE DIRECT PDF EDITOR • Change Names, Letters & Comments Instantly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Compact 3-pill feature row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniFeatureBadge(
                            icon = Icons.Default.PictureAsPdf,
                            label = "Edit PDF Free",
                            modifier = Modifier.weight(1f)
                        )
                        MiniFeatureBadge(
                            icon = Icons.Default.DocumentScanner,
                            label = "B&W Crop",
                            modifier = Modifier.weight(1f)
                        )
                        MiniFeatureBadge(
                            icon = Icons.Default.Translate,
                            label = "Share Status",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 1st Option: Guest Mode
                    GlassActionButton(
                        text = "Continue as Guest",
                        icon = Icons.Default.PersonOutline,
                        isPrimary = true,
                        height = 50.dp,
                        onClick = onContinueAsGuest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("continue_as_guest_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2nd Option: Google Login
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        }
                    } else {
                        GlassActionButton(
                            text = "Sign in with Google",
                            icon = Icons.Default.CloudDone,
                            isPrimary = false,
                            height = 50.dp,
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                launchSignInWithGoogleSheet(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_sign_in_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniFeatureBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        cornerRadius = 14.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
