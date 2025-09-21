package com.nirwanrn.thoughtcaddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.google.firebase.FirebaseApp
import com.nirwanrn.thoughtcaddy.navigation.ThoughtCaddyNavigation
import com.nirwanrn.thoughtcaddy.ui.theme.ThoughtCaddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ThoughtCaddyTheme {
                ThoughtCaddyNavigation()
            }
        }
    }
}