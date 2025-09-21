package com.nirwanrn.thoughtcaddy.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricAuthManager: BiometricAuthManager,
    private val biometricPreferences: BiometricPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
        checkBiometricAvailability()
        loadBiometricPreferences()
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }
            is AuthEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.password)
            }
            is AuthEvent.ConfirmPasswordChanged -> {
                _uiState.value = _uiState.value.copy(confirmPassword = event.confirmPassword)
            }
            is AuthEvent.Login -> {
                login()
            }
            is AuthEvent.Signup -> {
                signup()
            }
            is AuthEvent.Logout -> {
                logout()
            }
            is AuthEvent.ClearError -> {
                _uiState.value = _uiState.value.copy(authState = AuthState.Idle)
            }
            is AuthEvent.BiometricLogin -> {
                biometricLogin()
            }
            is AuthEvent.EnableBiometric -> {
                enableBiometric(event.enabled)
            }
            is AuthEvent.CheckBiometricAvailability -> {
                checkBiometricAvailability()
            }
            is AuthEvent.DeclineBiometric -> {
                declineBiometric()
            }
        }
    }

    private fun checkAuthState() {
        val isLoggedIn = authRepository.isUserLoggedIn()
        val currentUserEmail = authRepository.currentUser?.email ?: ""
        _uiState.value = _uiState.value.copy(
            isLoggedIn = isLoggedIn,
            email = if (isLoggedIn) currentUserEmail else _uiState.value.email
        )
    }

    private fun login() {
        val currentState = _uiState.value

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                authState = AuthState.Error("Please fill in all fields")
            )
            return
        }

        _uiState.value = currentState.copy(authState = AuthState.Loading)

        viewModelScope.launch {
            authRepository.signInWithEmailAndPassword(
                currentState.email,
                currentState.password
            ).fold(
                onSuccess = { user ->
                    _uiState.value = currentState.copy(
                        authState = AuthState.Success,
                        isLoggedIn = true,
                        email = user.email ?: currentState.email
                    )
                },
                onFailure = { exception ->
                    _uiState.value = currentState.copy(
                        authState = AuthState.Error(
                            exception.message ?: "Login failed"
                        )
                    )
                }
            )
        }
    }

    private fun signup() {
        val currentState = _uiState.value

        if (currentState.email.isBlank() ||
            currentState.password.isBlank() ||
            currentState.confirmPassword.isBlank()) {
            _uiState.value = currentState.copy(
                authState = AuthState.Error("Please fill in all fields")
            )
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _uiState.value = currentState.copy(
                authState = AuthState.Error("Passwords do not match")
            )
            return
        }

        if (currentState.password.length < 6) {
            _uiState.value = currentState.copy(
                authState = AuthState.Error("Password must be at least 6 characters")
            )
            return
        }

        _uiState.value = currentState.copy(authState = AuthState.Loading)

        viewModelScope.launch {
            authRepository.createUserWithEmailAndPassword(
                currentState.email,
                currentState.password
            ).fold(
                onSuccess = { user ->
                    _uiState.value = currentState.copy(
                        authState = AuthState.Success,
                        isLoggedIn = true,
                        email = user.email ?: currentState.email
                    )
                },
                onFailure = { exception ->
                    _uiState.value = currentState.copy(
                        authState = AuthState.Error(
                            exception.message ?: "Signup failed"
                        )
                    )
                }
            )
        }
    }

    private fun logout() {
        authRepository.signOut()
        biometricPreferences.clearBiometricData()
        _uiState.value = AuthUiState()
    }

    private fun loadBiometricPreferences() {
        val savedEmail = biometricPreferences.savedUserEmail
        val isBiometricEnabled = biometricPreferences.isBiometricEnabled
        val hasDeclinedBiometric = biometricPreferences.hasDeclinedBiometric

        _uiState.value = _uiState.value.copy(
            isBiometricEnabled = isBiometricEnabled,
            hasDeclinedBiometric = hasDeclinedBiometric,
            email = if (isBiometricEnabled && !savedEmail.isNullOrBlank()) savedEmail else _uiState.value.email
        )
    }

    private fun checkBiometricAvailability() {
        val availability = biometricAuthManager.isBiometricAvailable()
        _uiState.value = _uiState.value.copy(biometricAvailability = availability)
    }

    private fun biometricLogin() {
        val savedEmail = biometricPreferences.savedUserEmail
        if (savedEmail.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                authState = AuthState.Error("No saved email for biometric login")
            )
            return
        }

        // For biometric login, we assume the user is already authenticated via Firebase Auth persistence
        // This is a simplified implementation - in production you might want to re-authenticate with Firebase
        if (authRepository.isUserLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                authState = AuthState.Success,
                isLoggedIn = true,
                email = savedEmail
            )
        } else {
            _uiState.value = _uiState.value.copy(
                authState = AuthState.Error("Session expired. Please login with email and password")
            )
        }
    }

    private fun enableBiometric(enabled: Boolean) {
        val currentState = _uiState.value

        if (enabled) {
            // Save current user email for biometric login
            val currentEmail = authRepository.currentUser?.email
            if (currentEmail != null) {
                biometricPreferences.savedUserEmail = currentEmail
                biometricPreferences.isBiometricEnabled = true
                biometricPreferences.hasDeclinedBiometric = false // Clear declined flag

                _uiState.value = currentState.copy(
                    isBiometricEnabled = true,
                    hasDeclinedBiometric = false
                )
            } else {
                _uiState.value = currentState.copy(
                    authState = AuthState.Error("Please login first to enable biometric authentication")
                )
            }
        } else {
            biometricPreferences.isBiometricEnabled = false
            biometricPreferences.hasDeclinedBiometric = true

            _uiState.value = currentState.copy(
                isBiometricEnabled = false,
                hasDeclinedBiometric = true
            )
        }
    }

    private fun declineBiometric() {
        biometricPreferences.hasDeclinedBiometric = true
        _uiState.value = _uiState.value.copy(hasDeclinedBiometric = true)
    }

    // Function to handle biometric authentication with activity context
    fun authenticateWithBiometric(
        activity: androidx.fragment.app.FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (!biometricPreferences.isBiometricEnabled) {
            onError("Biometric authentication is not enabled")
            return
        }

        biometricAuthManager.authenticate(
            activity = activity,
            title = "ThoughtCaddy Authentication",
            subtitle = "Use your fingerprint or face to access your journal",
            onSuccess = {
                onEvent(AuthEvent.BiometricLogin)
                onSuccess()
            },
            onError = onError,
            onCancel = onCancel
        )
    }
}