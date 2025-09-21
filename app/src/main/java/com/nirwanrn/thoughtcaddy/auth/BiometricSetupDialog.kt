package com.nirwanrn.thoughtcaddy.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.nirwanrn.thoughtcaddy.ui.theme.ThoughtCaddyBlue

@Composable
fun BiometricSetupDialog(
    onDismiss: () -> Unit,
    onBiometricEnabled: () -> Unit,
    onBiometricDeclined: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isSettingUp by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { /* Prevent dismissing by tapping outside */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with glow effect
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ThoughtCaddyBlue.copy(alpha = 0.3f),
                                    ThoughtCaddyBlue.copy(alpha = 0.1f),
                                    ThoughtCaddyBlue.copy(alpha = 0.0f)
                                ),
                                radius = 120f
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Biometric Security",
                        modifier = Modifier.size(40.dp),
                        tint = ThoughtCaddyBlue
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Secure Your Journal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Would you like to use biometric authentication (fingerprint/face unlock) to secure your journal?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Quick and secure access\n• No need to remember passwords\n• Your data stays protected",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Enable Biometric Button
                Button(
                    onClick = {
                        isSettingUp = true
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            // Test biometric authentication first
                            authViewModel.authenticateWithBiometric(
                                activity = activity,
                                onSuccess = {
                                    // Biometric test successful, enable it
                                    authViewModel.onEvent(AuthEvent.EnableBiometric(true))
                                    isSettingUp = false
                                    onBiometricEnabled()
                                },
                                onError = { error ->
                                    // Biometric test failed
                                    isSettingUp = false
                                    // Still enable it in preferences so user can try again later
                                    authViewModel.onEvent(AuthEvent.EnableBiometric(true))
                                    onBiometricEnabled()
                                },
                                onCancel = {
                                    // User cancelled biometric test
                                    isSettingUp = false
                                    onBiometricDeclined()
                                }
                            )
                        } else {
                            isSettingUp = false
                            onBiometricDeclined()
                        }
                    },
                    enabled = !isSettingUp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThoughtCaddyBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSettingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Enable Biometric Security",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Decline Button
                TextButton(
                    onClick = {
                        // Mark that user has declined biometric
                        authViewModel.onEvent(AuthEvent.EnableBiometric(false))
                        onBiometricDeclined()
                    },
                    enabled = !isSettingUp,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maybe Later",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Note: If you choose 'Maybe Later', you'll need to log in each time you open the app.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}