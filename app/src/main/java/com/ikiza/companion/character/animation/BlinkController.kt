package com.ikiza.companion.character.animation

/**
 * Interface and architectural contract for anime eye blinking.
 *
 * NOTE ON ARTWORK & LIMITATIONS:
 * The official IKIZA companion artwork (`default_character.png`) is a single, flat,
 * fully merged illustration. It does not contain separate raster or vector layers for
 * the eye sockets, irises, whites, or upper/lower eyelashes.
 *
 * To achieve authentic, distortion-free eye blinking without visually damaging the face,
 * the following transparent PNG assets (matching the exact native resolution and pixel coordinates
 * of the character's eye region) are required:
 *
 * 1. `eyes_open.png`: Transparent overlay containing the default open purple eyes with highlights.
 * 2. `eyes_half.png`: Transparent overlay representing half-closed eyelids for intermediate blink frames.
 * 3. `eyes_closed.png`: Transparent overlay representing closed eyelids with eyelashes resting naturally.
 * 4. `base_face_empty_eyes.png`: Base character body with eye sockets prepared, or skin tone patches.
 *
 * Drawing artificial vector overlays over the merged artwork produces unnatural artifacts
 * and destroys face quality. Until layered artwork is provided, this controller operates
 * in fallback mode (safe eye preservation with full animation interface intact).
 */
interface BlinkController {
    /**
     * Whether authentic multi-layer blinking is currently possible with loaded assets.
     */
    val isLayeredBlinkSupported: Boolean

    /**
     * Normalized eyelid openness (1.0 = fully open, 0.5 = half, 0.0 = fully closed).
     */
    val eyelidOpenness: Float

    /**
     * Triggers a natural blink cycle if supported by character layers.
     */
    fun triggerBlink()
}

/**
 * Default implementation for single-image artwork that gracefully preserves original artwork.
 */
class SingleImageBlinkController : BlinkController {
    override val isLayeredBlinkSupported: Boolean = false
    override val eyelidOpenness: Float = 1.0f // Always preserve pristine original eyes

    override fun triggerBlink() {
        // No-op for single flat artwork to protect face integrity
    }
}
