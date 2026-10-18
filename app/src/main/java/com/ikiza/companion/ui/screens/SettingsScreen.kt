package com.ikiza.companion.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ikiza.companion.notifications.service.NotificationAccessHelper
import com.ikiza.companion.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()

    // Refresh access state when screen is viewed
    LaunchedEffect(Unit) {
        viewModel.refreshNotificationAccessStatus()
    }

    var newExcludedPackage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Notification Access Permission Card
            NotificationAccessCard(
                isGranted = isAccessGranted,
                onOpenSettings = {
                    context.startActivity(NotificationAccessHelper.createSettingsIntent())
                }
            )

            // 2. Voice Controls
            VoiceSettingsCard(
                voiceEnabled = settings.voiceEnabled,
                volume = settings.volume,
                pitch = settings.speechPitch,
                rate = settings.speechRate,
                onToggleVoice = { viewModel.toggleVoice() },
                onVolumeChange = { viewModel.setVolume(it) },
                onPitchChange = { viewModel.setSpeechPitch(it) },
                onRateChange = { viewModel.setSpeechRate(it) },
                onTestVoice = { viewModel.testVoice() }
            )

            // 3. Quiet Hours
            QuietHoursCard(
                enabled = settings.quietHoursEnabled,
                startHour = settings.quietHoursStartHour,
                startMinute = settings.quietHoursStartMinute,
                endHour = settings.quietHoursEndHour,
                endMinute = settings.quietHoursEndMinute,
                onToggle = { viewModel.setQuietHoursEnabled(it) },
                onTimeChanged = { sH, sM, eH, eM -> viewModel.setQuietHoursTime(sH, sM, eH, eM) }
            )

            // 4. Per-App Sender Announcement Toggles
            SenderNameConfigCard(
                senderPackages = settings.includeSenderPackages,
                onTogglePackage = { viewModel.toggleSenderPackage(it) }
            )

            // 5. Excluded Apps List
            ExcludedAppsCard(
                excludedPackages = settings.excludedPackages,
                newPackage = newExcludedPackage,
                onNewPackageChange = { newExcludedPackage = it },
                onAddPackage = {
                    viewModel.addExcludedPackage(newExcludedPackage)
                    newExcludedPackage = ""
                },
                onRemovePackage = { viewModel.removeExcludedPackage(it) }
            )

            // 6. On-Device Privacy Guarantee
            PrivacyGuaranteeCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationAccessCard(
    isGranted: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF81C784) else Color(0xFFFFA726),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isGranted) "Notification Access Granted" else "Notification Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "IKIZA requires Notification Listener permission to read incoming notification app names and sender headers so your companion can announce them. IKIZA has NO internet access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isGranted) "Re-check / Open System Settings" else "Grant Notification Access",
                    color = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }
        }
    }
}

@Composable
private fun VoiceSettingsCard(
    voiceEnabled: Boolean,
    volume: Float,
    pitch: Float,
    rate: Float,
    onToggleVoice: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onRateChange: (Float) -> Unit,
    onTestVoice: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Voice Announcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (voiceEnabled) "Companion voice is active" else "Quick Mute active (Silent)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Switch(checked = voiceEnabled, onCheckedChange = { onToggleVoice() })
            }

            if (voiceEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                // Volume Slider
                Text("Volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.0f..1.0f
                )

                // Pitch Slider
                Text("Voice Pitch: ${String.format("%.2f", pitch)}x", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = pitch,
                    onValueChange = onPitchChange,
                    valueRange = 0.7f..1.7f
                )

                // Speech Rate Slider
                Text("Speech Rate: ${String.format("%.2f", rate)}x", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = rate,
                    onValueChange = onRateChange,
                    valueRange = 0.7f..1.6f
                )

                Button(
                    onClick = onTestVoice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Voice Settings")
                }
            }
        }
    }
}

@Composable
private fun QuietHoursCard(
    enabled: Boolean,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    onToggle: (Boolean) -> Unit,
    onTimeChanged: (Int, Int, Int, Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Quiet Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (enabled) "Muted from %02d:%02d to %02d:%02d".format(startHour, startMinute, endHour, endMinute) else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Preset interval: %02d:%02d to %02d:%02d. Notifications arriving during quiet hours are silenced.".format(startHour, startMinute, endHour, endMinute),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = startHour == 22 && endHour == 7,
                        onClick = { onTimeChanged(22, 0, 7, 0) },
                        label = { Text("22:00 - 07:00", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = startHour == 23 && endHour == 8,
                        onClick = { onTimeChanged(23, 0, 8, 0) },
                        label = { Text("23:00 - 08:00", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = startHour == 0 && endHour == 6,
                        onClick = { onTimeChanged(0, 0, 6, 0) },
                        label = { Text("00:00 - 06:00", fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SenderNameConfigCard(
    senderPackages: Set<String>,
    onTogglePackage: (String) -> Unit
) {
    val commonApps = listOf(
        "WhatsApp" to "com.whatsapp",
        "Telegram" to "org.telegram.messenger",
        "Signal" to "org.thoughtcrime.securesms",
        "Slack" to "com.Slack"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Announce Sender Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Default is app name only ('Message from WhatsApp'). Tap an app below to include sender name ('Message from Alice on WhatsApp'):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonApps.forEach { (name, pkg) ->
                    val isIncluded = pkg in senderPackages
                    FilterChip(
                        selected = isIncluded,
                        onClick = { onTogglePackage(pkg) },
                        label = { Text(name) },
                        leadingIcon = if (isIncluded) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExcludedAppsCard(
    excludedPackages: Set<String>,
    newPackage: String,
    onNewPackageChange: (String) -> Unit,
    onAddPackage: () -> Unit,
    onRemovePackage: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Excluded Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Notifications from excluded packages will never trigger voice announcements.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPackage,
                    onValueChange = onNewPackageChange,
                    placeholder = { Text("e.g. com.spam.package", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAddPackage, enabled = newPackage.isNotBlank()) {
                    Text("Add")
                }
            }

            if (excludedPackages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    excludedPackages.forEach { pkg ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(pkg, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemovePackage(pkg) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyGuaranteeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "100% On-Device Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "IKIZA has zero internet permissions declared in its Android Manifest. All notification processing, rules filtering, settings storage, and voice synthesis happen strictly offline on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
