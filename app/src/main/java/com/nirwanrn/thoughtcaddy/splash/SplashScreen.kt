package com.nirwanrn.thoughtcaddy.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nirwanrn.thoughtcaddy.R
import com.nirwanrn.thoughtcaddy.auth.AuthViewModel
import com.nirwanrn.thoughtcaddy.ui.theme.AISummaryContainer
import com.nirwanrn.thoughtcaddy.ui.theme.ThoughtCaddyBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    onBiometricLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var biometricPromptShown by remember { mutableStateOf(false) }

    val logoScale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 100
        ),
        label = "logoScale"
    )

    val logoAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 100
        ),
        label = "logoAlpha"
    )

    val textAlpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 400
        ),
        label = "textAlpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1500) // Show initial animation for 1.5 seconds

        if (authState.biometricAvailability.isAvailable &&
            authState.isBiometricEnabled &&
            !authState.hasDeclinedBiometric &&
            !biometricPromptShown) {

            biometricPromptShown = true
            val activity = context as? FragmentActivity
            if (activity != null) {
                authViewModel.authenticateWithBiometric(
                    activity = activity,
                    onSuccess = {
                        onBiometricLoginSuccess()
                    },
                    onError = {
                        onSplashComplete()
                    },
                    onCancel = {
                        onSplashComplete()
                    }
                )
            } else {
                delay(1000)
                onSplashComplete()
            }
        } else if (authState.hasDeclinedBiometric &&
                   authState.biometricAvailability.isAvailable) {
            authViewModel.onEvent(com.nirwanrn.thoughtcaddy.auth.AuthEvent.Logout)
            delay(500)
            onSplashComplete()
        } else {
            // No biometric or normal flow
            delay(1000)
            onSplashComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AISummaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background,
                        ThoughtCaddyBlue.copy(alpha = 0.1f)
                    ),
                    radius = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with beautiful design
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow effect
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ThoughtCaddyBlue.copy(alpha = 0.3f),
                                    ThoughtCaddyBlue.copy(alpha = 0.1f),
                                    ThoughtCaddyBlue.copy(alpha = 0.05f)
                                ),
                                radius = 200f
                            ),
                            shape = CircleShape
                        )
                )

                // Main logo card
                Card(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_thought_caddy_logo),
                            contentDescription = "ThoughtCaddy Logo",
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name and Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
            ) {
                Text(
                    text = "ThoughtCaddy",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThoughtCaddyBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your AI-Powered Journal",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Capture thoughts • Gain insights • Reflect deeper",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .alpha(textAlpha.value),
                color = ThoughtCaddyBlue,
                strokeWidth = 3.dp
            )
        }
    }
}