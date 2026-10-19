package com.ikiza.companion.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ikiza.companion.ui.theme.AccentGreen
import com.ikiza.companion.ui.theme.AccentRose
import com.ikiza.companion.ui.theme.DarkBorder
import com.ikiza.companion.ui.theme.DarkSurface
import com.ikiza.companion.ui.theme.DarkSurfaceElevated
import com.ikiza.companion.ui.theme.TextMutedDark
import com.ikiza.companion.ui.theme.TextPrimaryDark
import com.ikiza.companion.ui.theme.TextSecondaryDark
import com.ikiza.companion.update.ApkInstaller
import com.ikiza.companion.update.ReleaseInfo
import com.ikiza.companion.update.UpdateStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscreetSettingsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    phoneNotificationsEnabled: Boolean,
    onTogglePhoneNotifications: () -> Unit,
    isNotificationAccessGranted: Boolean,
    characterBitmap: Bitmap?,
    ttsVoiceName: String?,
    ttsStatusMessage: String?,
    installedVersion: String,
    updateStatus: UpdateStatus,
    canInstallPackages: Boolean,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstall: (ReleaseInfo) -> Unit,
    onInstallDownloaded: (File) -> Unit,
    onRequestInstallPermission: () -> Unit,
    onResetUpdateStatus: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Companion Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                }
            }

            HorizontalDivider(color = DarkBorder)

            // SECTION 1: Exactly ONE top-level setting: Phone Notifications & Calls
            TopLevelSettingCard(
                enabled = phoneNotificationsEnabled,
                onToggle = onTogglePhoneNotifications,
                isAccessGranted = isNotificationAccessGranted,
                onOpenNotificationSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                },
                onRequestPhoneStatePermission = onRequestPhoneStatePermission
            )

            // SECTION 2: Official Companion Artwork (Bundled)
            CompanionArtworkCard(
                currentBitmap = characterBitmap
            )

            // SECTION 3: Voice Engine Details
            VoiceEngineCard(
                voiceName = ttsVoiceName,
                statusMessage = ttsStatusMessage
            )

            // SECTION 4: In-App Updates (Discreet Action)
            InAppUpdatesCard(
                installedVersion = installedVersion,
                updateStatus = updateStatus,
                canInstallPackages = canInstallPackages,
                onCheckForUpdates = onCheckForUpdates,
                onDownloadAndInstall = onDownloadAndInstall,
                onInstallDownloaded = onInstallDownloaded,
                onRequestInstallPermission = onRequestInstallPermission,
                onResetStatus = onResetUpdateStatus
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopLevelSettingCard(
    enabled: Boolean,
    onToggle: () -> Unit,
    isAccessGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (enabled) AccentRose else TextSecondaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Phone Notifications & Calls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (enabled) "Companion announces notifications" else "Announcements disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) AccentGreen else TextSecondaryDark
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentRose,
                        uncheckedThumbColor = TextSecondaryDark,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Notification Access Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAccessGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessGranted) AccentGreen else AccentRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAccessGranted) "Notification Access Granted" else "Notification Access Required",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAccessGranted) AccentGreen else AccentRose
                        )
                    }

                    if (!isAccessGranted) {
                        TextButton(onClick = onOpenNotificationSettings) {
                            Text("Enable", color = AccentRose, fontSize = 12.sp)
                        }
                    }
                }

                // Phone State Call Detection Access
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phone Call Detection",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    TextButton(onClick = onRequestPhoneStatePermission) {
                        Text("Grant Permission", color = AccentRose, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Announces incoming app notifications and phone calls using the female American English voice. No personal messages are read aloud.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CompanionArtworkCard(
    currentBitmap: Bitmap?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = AccentRose,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Companion Character",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Official Anime Illustration (Bundled)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen
                    )
                }
            }

            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Official artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, DarkBorder, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun VoiceEngineCard(
    voiceName: String?,
    statusMessage: String?
) {
    val context = LocalContext.current
    val isVoiceReady = voiceName != null &&
            !voiceName.contains("Disabled", ignoreCase = true) &&
            !voiceName.contains("Detecting", ignoreCase = true) &&
            statusMessage.isNullOrBlank()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (isVoiceReady) AccentGreen else AccentRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Companion Voice (en-US Female)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isVoiceReady) "Active Voice: $voiceName" else "Voice: Disabled (Strict en-US Female Required)",
                style = MaterialTheme.typography.bodySmall,
                color = if (isVoiceReady) AccentGreen else AccentRose
            )

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentRose,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val ttsIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(ttsIntent)
                        } catch (_: Exception) {
                            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(settingsIntent)
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Android TTS Settings", fontSize = 12.sp, color = TextPrimaryDark)
                }
            }
        }
    }
}

@Composable
private fun InAppUpdatesCard(
    installedVersion: String,
    updateStatus: UpdateStatus,
    canInstallPackages: Boolean,
    onCheckForUpdates: () -> Unit,
    onDownloadAndInstall: (ReleaseInfo) -> Unit,
    onInstallDownloaded: (File) -> Unit,
    onRequestInstallPermission: () -> Unit,
    onResetStatus: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = AccentRose, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "App Updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkBorder
                ) {
                    Text(
                        text = "v$installedVersion",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (updateStatus) {
                is UpdateStatus.Idle -> {
                    Text(
                        text = "Check GitHub Releases for newer companion versions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCheckForUpdates,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates", fontSize = 12.sp, color = TextPrimaryDark)
                    }
                }

                is UpdateStatus.Checking -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AccentRose,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Checking GitHub Releases...", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    }
                }

                is UpdateStatus.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("You're on the latest version (v${updateStatus.currentVersion})", color = AccentGreen, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCheckForUpdates) {
                        Text("Check Again", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }

                is UpdateStatus.Available -> {
                    val release = updateStatus.release
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Update Available: ${release.tagName}", fontWeight = FontWeight.SemiBold, color = AccentRose, fontSize = 13.sp)
                            if (release.apkSizeBytes > 0) {
                                val sizeMb = String.format("%.2f MB", release.apkSizeBytes / (1024.0 * 1024.0))
                                Text("Size: $sizeMb", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                            if (!release.releaseNotes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = release.releaseNotes.take(160),
                                    color = TextMutedDark,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onDownloadAndInstall(release) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download & Install", color = Color.White, fontSize = 12.sp)
                    }
                }

                is UpdateStatus.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Downloading update...", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            if (updateStatus.progressPercent >= 0) {
                                Text("${updateStatus.progressPercent}%", style = MaterialTheme.typography.bodySmall, color = AccentRose)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (updateStatus.progressPercent >= 0) {
                            LinearProgressIndicator(
                                progress = { updateStatus.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = AccentRose,
                                trackColor = DarkBorder
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = AccentRose,
                                trackColor = DarkBorder
                            )
                        }
                    }
                }

                is UpdateStatus.ReadyToInstall -> {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Download Complete!", fontWeight = FontWeight.SemiBold, color = AccentGreen, fontSize = 13.sp)
                            Text("Version: ${updateStatus.release.tagName}", color = TextSecondaryDark, fontSize = 11.sp)
                            Text("Verified SHA-256 integrity.", color = TextMutedDark, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!canInstallPackages) {
                        Button(
                            onClick = onRequestInstallPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow 'Install Unknown Apps' Permission", fontSize = 12.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Android requires explicit permission for IKIZA to install APK updates.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark
                        )
                    } else {
                        Button(
                            onClick = { onInstallDownloaded(updateStatus.apkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Install Update Now", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                is UpdateStatus.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRose, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(updateStatus.message, color = AccentRose, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onResetStatus) {
                        Text("Dismiss", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
