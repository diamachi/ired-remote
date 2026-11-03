package com.ired.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ired.app.ui.screens.HomeScreen
import com.ired.app.ui.theme.DarkBackground
import com.ired.app.ui.theme.IredTheme
import com.ired.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        setContent {
            IredTheme {
                // Paint the full edge-to-edge window, including the gesture-navigation
                // inset. The padded content Surface alone leaves that area to the
                // platform's gray navigation-bar scrim on some Android builds.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        color = DarkBackground
                    ) {
                        HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
