package com.ikiza.companion.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.ikiza.companion.character.CharacterDisplay
import com.ikiza.companion.character.animation.CompanionAnimationState
import com.ikiza.companion.ui.theme.AmoledBlack

/**
 * Main companion screen: Pure AMOLED black background.
 *
 * Requirements:
 * - Completely black on app open.
 * - Zero visible dashboard cards, headers, buttons, or menus.
 * - Double-tap reveals the official anime character with natural animations and female greeting.
 * - Long-press discreetly opens the settings modal without interfering with double-tap.
 * - Tap resets inactivity timer to keep companion awake.
 */
@Composable
fun BlackCompanionScreen(
    characterBitmap: Bitmap?,
    isCharacterVisible: Boolean,
    animationState: CompanionAnimationState = CompanionAnimationState.Idle,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        onTap()
                        onDoubleTap()
                    },
                    onLongPress = {
                        onTap()
                        onLongPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        CharacterDisplay(
            bitmap = characterBitmap,
            isVisible = isCharacterVisible,
            animationState = animationState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
