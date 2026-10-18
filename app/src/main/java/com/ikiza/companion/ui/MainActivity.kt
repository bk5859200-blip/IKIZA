package com.ikiza.companion.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ikiza.companion.ui.screens.MainCompanionScreen
import com.ikiza.companion.ui.screens.SettingsScreen
import com.ikiza.companion.ui.theme.IkizaTheme

enum class Screen {
    COMPANION,
    SETTINGS
}

/**
 * Main Activity hosting the IKIZA companion interface.
 * Keeps screen on so the companion remains visible on the desk stand beside your laptop.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while on the desk stand
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            IkizaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(Screen.COMPANION) }

                    BackHandler(enabled = currentScreen == Screen.SETTINGS) {
                        currentScreen = Screen.COMPANION
                    }

                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            Screen.COMPANION -> {
                                MainCompanionScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = Screen.COMPANION }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNotificationAccessStatus()
    }
}
