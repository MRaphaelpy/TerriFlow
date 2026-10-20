package com.mraphaelpy.terriflow.core.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `detecta versao mais nova`() {
        assertTrue(VersionComparator.isNewer("v1.0.5", "1.0.4"))
        assertTrue(VersionComparator.isNewer("1.1", "1.0.9"))
        assertTrue(VersionComparator.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `nao considera igual ou mais antiga como atualizacao`() {
        assertFalse(VersionComparator.isNewer("v1.0.4", "1.0.4"))
        assertFalse(VersionComparator.isNewer("1.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.3", "1.0.4"))
        assertFalse(VersionComparator.isNewer("latest", "1.0.4"))
    }

    @Test
    fun `remove prefixo v`() {
        assertEquals("1.2.3", VersionComparator.clean(" v1.2.3 "))
    }
}
