package com.ikiza.companion.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {

    private val sampleHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    @Test
    fun testExtractSha256FromReleaseBodyStandard() {
        val body = """
            ## Release v2.0.1
            - Polished animations
            - Checksum SHA-256: $sampleHash
        """.trimIndent()

        val extracted = UpdateChecker.extractSha256(body)
        assertEquals(sampleHash, extracted)
    }

    @Test
    fun testExtractSha256CaseInsensitiveAndVariations() {
        val variations = listOf(
            "sha256: $sampleHash",
            "SHA256: $sampleHash",
            "SHA-256 = $sampleHash",
            "Checksum: $sampleHash",
            "checksum: $sampleHash"
        )

        for (text in variations) {
            val extracted = UpdateChecker.extractSha256(text)
            assertEquals("Failed for text: $text", sampleHash, extracted)
        }
    }

    @Test
    fun testExtractSha256ReturnsNullWhenNoChecksum() {
        assertNull(UpdateChecker.extractSha256("Just bug fixes and improvements."))
        assertNull(UpdateChecker.extractSha256(null))
        assertNull(UpdateChecker.extractSha256(""))
    }

    @Test
    fun testParseChecksumFileContentStandardSha256sum() {
        val standardOutput = "$sampleHash  IKIZA-v2-debug.apk"
        val parsed = UpdateChecker.parseChecksumFileContent(standardOutput)
        assertEquals(sampleHash, parsed)

        val binaryModeOutput = "$sampleHash *IKIZA-v2-debug.apk\n"
        assertEquals(sampleHash, UpdateChecker.parseChecksumFileContent(binaryModeOutput))
    }

    @Test
    fun testParseChecksumFileContentBareHash() {
        assertEquals(sampleHash, UpdateChecker.parseChecksumFileContent(sampleHash))
        assertEquals(sampleHash, UpdateChecker.parseChecksumFileContent("   $sampleHash   \n"))
    }

    @Test
    fun testParseChecksumFileContentInvalid() {
        assertNull(UpdateChecker.parseChecksumFileContent("not-a-hash"))
        assertNull(UpdateChecker.parseChecksumFileContent(null))
        assertNull(UpdateChecker.parseChecksumFileContent(""))
    }
}
