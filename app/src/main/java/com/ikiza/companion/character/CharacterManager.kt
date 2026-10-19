package com.ikiza.companion.character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * Manages loading of the official bundled character artwork.
 * In this version, the official bundled character is the only active character.
 */
object CharacterManager {

    private const val TAG = "IKIZA_CharacterManager"

    /**
     * Loads the official bundled companion character Bitmap.
     */
    fun loadCharacterBitmap(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, com.ikiza.companion.R.drawable.default_character)
        } catch (_: Exception) {
            val defaultResId = context.resources.getIdentifier("default_character", "drawable", context.packageName)
            if (defaultResId != 0) {
                try {
                    BitmapFactory.decodeResource(context.resources, defaultResId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode bundled default_character drawable: ${e.message}")
                    null
                }
            } else {
                null
            }
        }
    }
}
