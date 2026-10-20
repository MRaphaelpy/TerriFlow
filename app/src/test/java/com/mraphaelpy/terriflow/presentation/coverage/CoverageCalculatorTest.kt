package com.mraphaelpy.terriflow.presentation.coverage

import com.mraphaelpy.terriflow.core.report.ReportRow
import com.mraphaelpy.terriflow.domain.model.AssignmentHistory
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.ServiceYear
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CoverageCalculatorTest {

    private val now = Date()
    private val never = Fixtures.territory().copy(id = "never", code = "T-00001")
    private val old = Fixtures.territory().copy(id = "old", code = "T-00002")
    private val recent = Fixtures.territory().copy(id = "recent", code = "T-00003")

    private val events = listOf(
        assigned("old", 500), completed("old", 400),
        assigned("recent", 60), completed("recent", 50),
        assigned("recent", 20), completed("recent", 5)
    )
    private val periods = AssignmentHistory.build(events)
    private val year = ServiceYear.of(now)
    private val coverages = CoverageCalculator.compute(listOf(never, old, recent), periods, year)

    @Test
    fun `calcula ultima conclusao, quantidade e media`() {
        val recentCoverage = coverages.first { it.territory.id == "recent" }
        assertEquals(2, recentCoverage.completionCount)
        assertEquals(Fixtures.daysAgo(5, now), recentCoverage.lastCompletedDate)
        assertEquals(12.5, recentCoverage.averageDaysToComplete!!, 0.01)
    }

    @Test
    fun `ordenacoes partem sempre da lista completa`() {
        assertEquals(listOf("never"), CoverageCalculator.sort(coverages, CoverageSortOption.NEVER_WORKED).ids())
        assertEquals(listOf("never", "old"), CoverageCalculator.sort(coverages, CoverageSortOption.OVER_12_MONTHS, now).ids())
        assertEquals(listOf("never", "old", "recent"), CoverageCalculator.sort(coverages, CoverageSortOption.OLDEST_COMPLETED_FIRST).ids())
        assertEquals(listOf("recent", "old", "never"), CoverageCalculator.sort(coverages, CoverageSortOption.NEWEST_COMPLETED_FIRST).ids())
    }

    @Test
    fun `s13 agrupa por territorio e marca nao trabalhados`() {
        val table = S13ReportBuilder.build(listOf(never, recent), periods, year, "Congregação Teste")
        val groups = table.rows.filterIsInstance<ReportRow.Group>()
        assertEquals(2, groups.size)
        assertTrue(groups.first().title.startsWith("T-00001"))
        assertTrue(table.rows.filterIsInstance<ReportRow.Cells>().any { "Não trabalhado no ano" in it.values })
        assertTrue(table.subtitle.contains("Congregação Teste"))
    }

    private fun List<TerritoryCoverage>.ids() = map { it.territory.id }

    private fun assigned(territoryId: String, daysAgo: Long) = TerritoryEvent(
        id = "a-$territoryId-$daysAgo", territoryId = territoryId, type = EventType.ASSIGNED,
        timestamp = Fixtures.daysAgo(daysAgo, now), extra = mapOf("responsibleId" to "pub", "responsibleName" to "Publicador")
    )

    private fun completed(territoryId: String, daysAgo: Long) = TerritoryEvent(
        id = "c-$territoryId-$daysAgo", territoryId = territoryId, type = EventType.COMPLETED,
        timestamp = Fixtures.daysAgo(daysAgo, now)
    )
}
