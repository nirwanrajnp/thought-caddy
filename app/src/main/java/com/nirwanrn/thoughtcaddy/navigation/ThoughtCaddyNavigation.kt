package com.nirwanrn.thoughtcaddy.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.nirwanrn.thoughtcaddy.auth.AuthViewModel
import com.nirwanrn.thoughtcaddy.auth.BiometricSetupDialog
import com.nirwanrn.thoughtcaddy.auth.LoginScreen
import com.nirwanrn.thoughtcaddy.auth.SignupScreen
import com.nirwanrn.thoughtcaddy.main.MainScreen
import com.nirwanrn.thoughtcaddy.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Journal : Screen("journal")
}

fun NavHostController.navigateToLogin() {
    navigate(Screen.Login.route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

fun NavHostController.navigateToSignup() {
    navigate(Screen.Signup.route)
}

fun NavHostController.navigateToJournal() {
    navigate(Screen.Journal.route) {
        popUpTo(Screen.Login.route) { inclusive = true }
    }
}

@Composable
fun ThoughtCaddyNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val initialDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Screen.Journal.route
    } else {
        Screen.Login.route
    }

    LaunchedEffect(uiState.isLoggedIn) {
        val currentRoute = navController.currentDestination?.route

        when {
            uiState.isLoggedIn && currentRoute != Screen.Journal.route -> {
                navController.navigateToJournal()
            }
            !uiState.isLoggedIn && currentRoute == Screen.Journal.route -> {
                navController.navigateToLogin()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = initialDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigateToSignup()
                },
                onLoginSuccess = {
                    navController.navigateToJournal()
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignupSuccess = {
                    navController.navigateToJournal()
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.Journal.route) {
            MainScreen(
                onLogout = {
                    authViewModel.onEvent(com.nirwanrn.thoughtcaddy.auth.AuthEvent.Logout)
                    navController.navigateToLogin()
                },
                authViewModel = authViewModel
            )
        }
    }
}