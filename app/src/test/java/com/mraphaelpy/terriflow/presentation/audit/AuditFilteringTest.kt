package com.mraphaelpy.terriflow.presentation.audit

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class AuditFilteringTest {

    private val now = Date()
    private val transfer = TerritoryEvent(
        id = "e1", territoryId = "t1", userId = "admin", type = EventType.TRANSFERRED,
        timestamp = Fixtures.daysAgo(3, now), extra = mapOf("toUserId" to "pub")
    )

    @Test
    fun `filtra por periodo, tipo, territorio e pessoa envolvida`() {
        assertTrue(AuditFilter().matches(transfer, now))
        assertFalse(AuditFilter().matches(transfer.copy(timestamp = Fixtures.daysAgo(40, now)), now))
        assertTrue(AuditFilter(period = AuditPeriod.ALL).matches(transfer.copy(timestamp = Fixtures.daysAgo(400, now)), now))
        assertFalse(AuditFilter(type = EventType.ASSIGNED).matches(transfer, now))
        assertFalse(AuditFilter(territoryId = "t2").matches(transfer, now))
        assertTrue(AuditFilter(userId = "pub").matches(transfer, now))
    }

    @Test
    fun `agrupa por dia com hoje e ontem`() {
        val entries = listOf(
            AuditEntry(transfer.copy(id = "a", timestamp = now), null),
            AuditEntry(transfer.copy(id = "b", timestamp = Fixtures.daysAgo(1, now)), null),
            AuditEntry(transfer.copy(id = "c", timestamp = Fixtures.daysAgo(5, now)), null)
        )
        val days = AuditGrouping.byDay(entries, now)
        assertEquals(listOf("Hoje", "Ontem"), days.take(2).map { it.label })
        assertEquals(3, days.size)
    }

    @Test
    fun `descreve alteracoes de edicao`() {
        val event = TerritoryEvent(type = EventType.UPDATED, extra = mapOf("name" to "Novo", "nameFrom" to "Antigo", "pinChanged" to "true"))
        assertEquals(listOf("Nome: Antigo → Novo", "Localização no mapa alterada"), describeChanges(event))
    }
}
