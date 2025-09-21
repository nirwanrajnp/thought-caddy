package com.nirwanrn.thoughtcaddy.auth

sealed class AuthState {
    object Loading : AuthState()
    object Idle : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class AuthUiState(
    val authState: AuthState = AuthState.Idle,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoggedIn: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.Unknown,
    val hasDeclinedBiometric: Boolean = false
)

sealed class AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent()
    object Login : AuthEvent()
    object Signup : AuthEvent()
    object Logout : AuthEvent()
    object ClearError : AuthEvent()
    object BiometricLogin : AuthEvent()
    data class EnableBiometric(val enabled: Boolean) : AuthEvent()
    object DeclineBiometric : AuthEvent()
    object CheckBiometricAvailability : AuthEvent()
}