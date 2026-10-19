package com.ikiza.companion.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsVoiceManagerTest {

    @Test
    fun testRecognizesGoogleFemaleVoices() {
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-us-x-sfg#female_1-local"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-us-x-tpd#female_2-local"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-us-x-iob-network"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-us-x-iol-local"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-us-x-iom-local"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-us-x-tpc-local"))
    }

    @Test
    fun testRecognizesGenericFemaleTaggedVoices() {
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en-US-language-female"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("en_us_female_high_quality"))
        assertTrue(TtsVoiceManager.isStrictFemaleVoice("standard-voice", setOf("gender:female")))
    }

    @Test
    fun testStrictlyRejectsMaleVoices() {
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en-us-x-iog-local"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en-us-x-ioj-network"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en-us-x-iok-local"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en-us-x-tpf-local"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en-US-male-voice"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en_us_male_1"))
    }

    @Test
    fun testStrictlyRejectsUnspecifiedOrGenericDefaultVoices() {
        // A generic voice without "female" tag must be refused to avoid silent male fallback
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en-US-language"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("en_us_default"))
        assertFalse(TtsVoiceManager.isStrictFemaleVoice("tts-engine-generic"))
    }

    @Test
    fun testIsExplicitlyMaleHelper() {
        assertTrue(TtsVoiceManager.isExplicitlyMale("male_voice"))
        assertTrue(TtsVoiceManager.isExplicitlyMale("en-us-x-iog"))
        assertFalse(TtsVoiceManager.isExplicitlyMale("female_voice"))
        assertFalse(TtsVoiceManager.isExplicitlyMale("en-us-x-sfg"))
    }
}
