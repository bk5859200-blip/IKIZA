package com.ikiza.companion.ui

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ikiza.companion.character.CharacterManager
import com.ikiza.companion.character.animation.CompanionAnimationController
import com.ikiza.companion.character.animation.CompanionAnimationState
import com.ikiza.companion.notifications.CallStateMonitor
import com.ikiza.companion.notifications.CompanionBridge
import com.ikiza.companion.notifications.CompanionNotificationService
import com.ikiza.companion.settings.AppSettings
import com.ikiza.companion.settings.SettingsRepository
import com.ikiza.companion.update.ApkDownloader
import com.ikiza.companion.update.ApkInstaller
import com.ikiza.companion.update.CheckResult
import com.ikiza.companion.update.ReleaseInfo
import com.ikiza.companion.update.UpdateChecker
import com.ikiza.companion.update.UpdateStatus
import com.ikiza.companion.voice.TtsVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class CompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    val speechManager = TtsVoiceManager(application)
    val animationController = CompanionAnimationController(viewModelScope)
    val animationState: StateFlow<CompanionAnimationState> = animationController.state

    private val updateChecker = UpdateChecker()
    private val apkDownloader = ApkDownloader(application)
    private val callStateMonitor: CallStateMonitor

    val installedVersionName: String = try {
        val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        pInfo.versionName ?: "2.0.0"
    } catch (_: Exception) {
        "2.0.0"
    }

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _characterBitmap = MutableStateFlow<Bitmap?>(null)
    val characterBitmap: StateFlow<Bitmap?> = _characterBitmap.asStateFlow()

    private val _isCharacterVisible = MutableStateFlow(false)
    val isCharacterVisible: StateFlow<Boolean> = _isCharacterVisible.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _canInstallPackages = MutableStateFlow(false)
    val canInstallPackages: StateFlow<Boolean> = _canInstallPackages.asStateFlow()

    init {
        // Load official bundled character artwork
        _characterBitmap.value = CharacterManager.loadCharacterBitmap(application)

        // Sync speech state with animation controller
        viewModelScope.launch {
            speechManager.isSpeakingFlow.collect { isSpeaking ->
                animationController.onSpeechStateChanged(isSpeaking)
            }
        }

        // Listen for announcements posted by notification listener service
        viewModelScope.launch {
            CompanionBridge.announcements.collect { announcement ->
                if (settings.value.phoneNotificationsEnabled) {
                    animationController.onNotificationReceived()
                    speechManager.speakAnnouncement(announcement)
                }
            }
        }

        // Telephony call monitor
        callStateMonitor = CallStateMonitor(application) { callAnnouncement ->
            if (settings.value.phoneNotificationsEnabled) {
                animationController.onCallStateChanged(true)
                speechManager.speakAnnouncement(callAnnouncement)
            }
        }
        callStateMonitor.start()

        refreshPermissions()
    }

    fun refreshPermissions() {
        val app = getApplication<Application>()
        _canInstallPackages.value = ApkInstaller.canRequestPackageInstalls(app)

        // Check if Notification Listener is granted
        val enabledListeners = Settings.Secure.getString(
            app.contentResolver,
            "enabled_notification_listeners"
        )
        val cn = ComponentName(app, CompanionNotificationService::class.java)
        _isNotificationAccessGranted.value = enabledListeners?.contains(cn.flattenToString()) == true
    }

    fun onUserInteraction() {
        animationController.onUserInteraction()
    }

    fun onDoubleTap() {
        _isCharacterVisible.value = true
        animationController.onGreeting()
        speechManager.speakGreeting()
    }

    fun onLongPress() {
        refreshPermissions()
        _isSettingsOpen.value = true
    }

    fun openSettings() {
        refreshPermissions()
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun togglePhoneNotifications() {
        viewModelScope.launch {
            val current = settings.value.phoneNotificationsEnabled
            settingsRepository.setPhoneNotificationsEnabled(!current)
        }
    }

    fun checkForUpdates() {
        _updateStatus.value = UpdateStatus.Checking
        animationController.onTaskStateChanged(true)
        viewModelScope.launch {
            try {
                when (val result = updateChecker.check(installedVersionName)) {
                    is CheckResult.UpdateAvailable -> {
                        _updateStatus.value = UpdateStatus.Available(result.release, installedVersionName)
                    }
                    is CheckResult.UpToDate -> {
                        _updateStatus.value = UpdateStatus.UpToDate(installedVersionName)
                    }
                    is CheckResult.NoReleaseFound -> {
                        _updateStatus.value = UpdateStatus.UpToDate(installedVersionName)
                    }
                    is CheckResult.Error -> {
                        _updateStatus.value = UpdateStatus.Error(result.message)
                    }
                }
            } finally {
                animationController.onTaskStateChanged(false)
            }
        }
    }

    fun downloadAndInstallUpdate(release: ReleaseInfo) {
        _updateStatus.value = UpdateStatus.Downloading(0, 0L, release.apkSizeBytes)
        animationController.onTaskStateChanged(true)
        viewModelScope.launch {
            try {
                val file = apkDownloader.download(
                    downloadUrl = release.apkDownloadUrl,
                    targetFileName = release.apkFileName,
                    expectedSha256 = release.expectedSha256,
                    sha256AssetUrl = release.sha256AssetUrl
                ) { percent, downloaded, total ->
                    _updateStatus.value = UpdateStatus.Downloading(percent, downloaded, total)
                }

                _updateStatus.value = UpdateStatus.ReadyToInstall(file, release)

                if (ApkInstaller.canRequestPackageInstalls(getApplication())) {
                    ApkInstaller.installApk(getApplication(), file)
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error("Download failed: ${e.localizedMessage}")
            } finally {
                animationController.onTaskStateChanged(false)
            }
        }
    }

    fun installDownloadedApk(file: File) {
        try {
            ApkInstaller.installApk(getApplication(), file)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Install failed: ${e.localizedMessage}")
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    override fun onCleared() {
        super.onCleared()
        animationController.stop()
        callStateMonitor.stop()
        speechManager.shutdown()
    }
}
