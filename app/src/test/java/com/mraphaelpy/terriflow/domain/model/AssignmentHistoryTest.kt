package com.mraphaelpy.terriflow.domain.model

import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class AssignmentHistoryTest {

    private val now = Date()
    private fun event(type: EventType, daysAgo: Long, extra: Map<String, String> = emptyMap(), territory: String = "t1") =
        TerritoryEvent(
            id = "$type-$daysAgo-$territory",
            territoryId = territory,
            userId = "admin",
            userName = "Admin",
            type = type,
            timestamp = Fixtures.daysAgo(daysAgo, now),
            extra = extra
        )

    @Test
    fun `reconstroi designacoes, transferencia e devolucao`() {
        val events = listOf(
            event(EventType.ASSIGNED, 100, mapOf("responsibleId" to "a", "responsibleName" to "Ana")),
            event(EventType.STARTED, 99),
            event(EventType.COMPLETED, 60),
            event(EventType.TRANSFERRED, 60, mapOf("toUserId" to "b", "toUserName" to "Bia")),
            event(EventType.RETURNED, 30),
            event(EventType.ASSIGNED, 10, mapOf("responsibleId" to "c", "responsibleName" to "Caio"))
        )

        val periods = AssignmentHistory.build(events).getValue("t1")

        assertEquals(listOf("Ana", "Bia", "Caio"), periods.map { it.responsibleName })
        assertEquals(40L, periods[0].durationDays(now))
        assertTrue(periods[0].isCompleted)
        assertEquals(Fixtures.daysAgo(30, now), periods[1].returnedAt)
        assertTrue(periods[2].isOpen)
    }

    @Test
    fun `conclusao sem designacao registrada ainda conta`() {
        val periods = AssignmentHistory.build(listOf(event(EventType.COMPLETED, 5))).getValue("t1")
        assertNull(periods.single().assignedAt)
        assertTrue(periods.single().isCompleted)
    }

    @Test
    fun `estatisticas de cobertura e por dirigente`() {
        val t1 = Fixtures.territory().copy(id = "t1")
        val t2 = Fixtures.territory().copy(id = "t2", code = "T-00002")
        val events = listOf(
            event(EventType.ASSIGNED, 20, mapOf("responsibleId" to "a", "responsibleName" to "Ana")),
            event(EventType.COMPLETED, 10)
        )
        val periods = AssignmentHistory.build(events)

        val coverage = TerritoryStatistics.coverage(listOf(t1, t2), periods, ServiceYear.of(Fixtures.daysAgo(10, now)))
        assertEquals(listOf(t1), coverage.worked)
        assertEquals(listOf(t2), coverage.pending)
        assertEquals(50, coverage.percent)

        assertEquals(listOf(t2), TerritoryStatistics.notCompletedSince(listOf(t1, t2), periods, now = now))

        val ana = TerritoryStatistics.byResponsible(periods, listOf(t1, t2)).single()
        assertEquals(1, ana.completed)
        assertEquals(10.0, ana.averageDaysToComplete!!, 0.01)
    }
}
