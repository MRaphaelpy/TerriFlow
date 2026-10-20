package com.mraphaelpy.terriflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class ServiceYearTest {

    private fun date(year: Int, month: Int, day: Int): Date =
        Calendar.getInstance().apply { clear(); set(year, month, day, 12, 0) }.time

    @Test
    fun `ano de servico comeca em setembro`() {
        assertEquals(ServiceYear(2025), ServiceYear.of(date(2025, Calendar.SEPTEMBER, 1)))
        assertEquals(ServiceYear(2025), ServiceYear.of(date(2026, Calendar.AUGUST, 31)))
        assertEquals(ServiceYear(2024), ServiceYear.of(date(2025, Calendar.AUGUST, 31)))
        assertEquals("2025/2026", ServiceYear(2025).label)
    }

    @Test
    fun `contains respeita os limites`() {
        val year = ServiceYear(2025)
        assertTrue(date(2025, Calendar.SEPTEMBER, 1) in year)
        assertTrue(date(2026, Calendar.AUGUST, 31) in year)
        assertFalse(date(2026, Calendar.SEPTEMBER, 1) in year)
    }
}
