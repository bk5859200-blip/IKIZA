package com.ikiza.companion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ikiza.companion.character.animation.rememberAnimatedCharacterState
import com.ikiza.companion.character.ui.CharacterView
import com.ikiza.companion.core.model.CompanionExpression
import com.ikiza.companion.ui.MainViewModel

/**
 * Main companion screen designed for a phone sitting in portrait orientation on a desk stand.
 */
@Composable
fun MainCompanionScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val currentExpression by viewModel.currentExpression.collectAsState()
    val recentAnnouncement by viewModel.recentAnnouncement.collectAsState()
    val isAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()

    // Realtime animation state driven by idle breath, blink cycle, and speech
    val characterState = rememberAnimatedCharacterState(
        targetExpression = currentExpression,
        isSpeaking = speechState.isSpeaking
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Status, Quick Mute, and Settings
        TopStatusBar(
            isAccessGranted = isAccessGranted,
            isMuted = !settings.voiceEnabled,
            onToggleMute = { viewModel.toggleVoice() },
            onOpenSettings = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Speech Bubble
        SpeechBubble(
            text = speechState.currentText ?: recentAnnouncement?.textToSpeak ?: "Standing by on your desk stand...",
            isSpeaking = speechState.isSpeaking
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Center: Anime Character
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CharacterView(state = characterState)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Recent Notification Banner (if any)
        AnimatedVisibility(
            visible = recentAnnouncement != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            recentAnnouncement?.let { announcement ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = announcement.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Expression Preview Chips
        Text(
            text = "Companion Expression",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CompanionExpression.values()) { expr ->
                FilterChip(
                    selected = currentExpression == expr,
                    onClick = { viewModel.setExpression(expr, autoResetMs = 4000L) },
                    label = { Text(expr.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.testVoice() }) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Voice", fontSize = 13.sp)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = if (speechState.isSpeaking) "Speaking..." else "Idle",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (speechState.isSpeaking) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TopStatusBar(
    isAccessGranted: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notification Access Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAccessGranted) Color(0xFF2E7D32).copy(alpha = 0.25f) else Color(0xFFC62828).copy(alpha = 0.25f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAccessGranted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = if (isAccessGranted) Color(0xFF81C784) else Color(0xFFE57373),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAccessGranted) "Listening" else "Listener Off",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAccessGranted) Color(0xFF81C784) else Color(0xFFE57373)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute voice" else "Mute voice",
                    tint = if (isMuted) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SpeechBubble(
    text: String,
    isSpeaking: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeaking) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSpeaking) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                fontWeight = if (isSpeaking) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
