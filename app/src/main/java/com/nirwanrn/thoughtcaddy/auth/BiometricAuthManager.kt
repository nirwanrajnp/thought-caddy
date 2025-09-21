package com.nirwanrn.thoughtcaddy.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check if biometric authentication is available on the device
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)

        // Try BIOMETRIC_WEAK first (more permissive)
        val weakResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (weakResult == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricAvailability.Available
        }

        // Try BIOMETRIC_STRONG
        val strongResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (strongResult == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricAvailability.Available
        }

        // Try combined biometric and device credential
        val combinedResult = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (combinedResult == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricAvailability.Available
        }

        // Return the most specific error from BIOMETRIC_WEAK check
        return when (weakResult) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.Unsupported
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.Unknown
            else -> BiometricAvailability.Unknown
        }
    }

    /**
     * Authenticate using biometrics
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Use your fingerprint or face to authenticate",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val availability = isBiometricAvailable()
        if (availability != BiometricAvailability.Available) {
            onError("Biometric not available: ${availability.message}")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                    else -> onError(errString.toString())
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed. Please try again.")
            }
        })

        // Use BIOMETRIC_WEAK for broader compatibility
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

/**
 * Enum representing the availability of biometric authentication
 */
enum class BiometricAvailability {
    Available,
    NoHardware,
    HardwareUnavailable,
    NoneEnrolled,
    SecurityUpdateRequired,
    Unsupported,
    Unknown;

    val isAvailable: Boolean
        get() = this == Available

    val message: String
        get() = when (this) {
            Available -> "Biometric authentication is available"
            NoHardware -> "This device doesn't have biometric hardware"
            HardwareUnavailable -> "Biometric hardware is currently unavailable"
            NoneEnrolled -> "No biometric credentials are enrolled. Please set up fingerprint or face unlock in your device settings"
            SecurityUpdateRequired -> "A security update is required for biometric authentication"
            Unsupported -> "Biometric authentication is not supported on this device"
            Unknown -> "Biometric authentication status is unknown"
        }
}