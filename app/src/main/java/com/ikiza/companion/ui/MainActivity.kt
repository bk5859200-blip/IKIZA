package com.ikiza.companion.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ikiza.companion.ui.screens.BlackCompanionScreen
import com.ikiza.companion.ui.screens.DiscreetSettingsSheet
import com.ikiza.companion.ui.theme.IkizaTheme
import com.ikiza.companion.update.ApkInstaller
import com.ikiza.companion.update.UpdateNotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: CompanionViewModel by viewModels()

    // Runtime permission launcher for Phone Calls detection (READ_PHONE_STATE)
    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            IkizaTheme {
                val characterBitmap by viewModel.characterBitmap.collectAsState()
                val isCharacterVisible by viewModel.isCharacterVisible.collectAsState()
                val animationState by viewModel.animationState.collectAsState()
                val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                val canInstallPackages by viewModel.canInstallPackages.collectAsState()
                val voiceName by viewModel.speechManager.selectedVoiceName.collectAsState()
                val voiceStatusMessage by viewModel.speechManager.voiceStatusMessage.collectAsState()

                // Main pure black AMOLED companion screen
                BlackCompanionScreen(
                    characterBitmap = characterBitmap,
                    isCharacterVisible = isCharacterVisible,
                    animationState = animationState,
                    onTap = { viewModel.onUserInteraction() },
                    onDoubleTap = { viewModel.onDoubleTap() },
                    onLongPress = { viewModel.onLongPress() }
                )

                // Discreet modal settings sheet
                DiscreetSettingsSheet(
                    isOpen = isSettingsOpen,
                    onDismiss = { viewModel.closeSettings() },
                    phoneNotificationsEnabled = settings.phoneNotificationsEnabled,
                    onTogglePhoneNotifications = { viewModel.togglePhoneNotifications() },
                    isNotificationAccessGranted = isNotificationAccessGranted,
                    characterBitmap = characterBitmap,
                    ttsVoiceName = voiceName,
                    ttsStatusMessage = voiceStatusMessage,
                    installedVersion = viewModel.installedVersionName,
                    updateStatus = updateStatus,
                    canInstallPackages = canInstallPackages,
                    onCheckForUpdates = { viewModel.checkForUpdates() },
                    onDownloadAndInstall = { release -> viewModel.downloadAndInstallUpdate(release) },
                    onInstallDownloaded = { file -> viewModel.installDownloadedApk(file) },
                    onRequestInstallPermission = {
                        startActivity(ApkInstaller.createInstallPermissionIntent(this))
                    },
                    onResetUpdateStatus = { viewModel.resetUpdateStatus() },
                    onRequestPhoneStatePermission = {
                        phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UpdateNotificationHelper.ACTION_OPEN_UPDATES) {
            viewModel.openSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.speechManager.recheckVoiceConfiguration()
    }
}
