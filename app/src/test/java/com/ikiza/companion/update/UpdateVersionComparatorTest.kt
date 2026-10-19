package com.ikiza.companion.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionComparatorTest {

    @Test
    fun testNewerPatchVersion() {
        assertTrue(UpdateVersionComparator.isNewer("v2.0.1", "2.0.0"))
        assertTrue(UpdateVersionComparator.isNewer("2.0.1", "2.0.0"))
    }

    @Test
    fun testNumericalComparisonHandlesMultiDigitCorrectly() {
        // Alphabetically "2.10.0" < "2.2.0", but numerically 2.10.0 > 2.2.0!
        assertTrue(UpdateVersionComparator.isNewer("2.10.0", "2.2.0"))
        assertFalse(UpdateVersionComparator.isNewer("2.2.0", "2.10.0"))
    }

    @Test
    fun testIdenticalVersionsAreNotNewer() {
        assertFalse(UpdateVersionComparator.isNewer("2.0.0", "2.0.0"))
        assertFalse(UpdateVersionComparator.isNewer("v2.0.0", "2.0.0"))
    }

    @Test
    fun testOlderVersionReturnsFalse() {
        assertFalse(UpdateVersionComparator.isNewer("v1.9.9", "2.0.0"))
        assertFalse(UpdateVersionComparator.isNewer("2.0.0", "2.1.0"))
    }

    @Test
    fun testPreReleaseSuffixesAreSanitized() {
        assertTrue(UpdateVersionComparator.isNewer("v2.1.0-beta", "2.0.0"))
        assertFalse(UpdateVersionComparator.isNewer("v2.0.0-rc1", "2.0.0"))
    }
}
