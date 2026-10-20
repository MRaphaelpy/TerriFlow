package com.mraphaelpy.terriflow.presentation.coverage

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverageCalculatorTest {

    private val never = Fixtures.territory().copy(id = "never", code = "T-00001")
    private val old = Fixtures.territory().copy(id = "old", code = "T-00002")
    private val recent = Fixtures.territory().copy(id = "recent", code = "T-00003")

    private val events = listOf(
        completed("old", daysAgo = 100),
        completed("recent", daysAgo = 5),
        completed("recent", daysAgo = 50)
    )

    private val coverages = CoverageCalculator.compute(listOf(never, old, recent), events)

    @Test
    fun `calcula ultima conclusao e quantidade`() {
        val recentCoverage = coverages.first { it.territory.id == "recent" }
        assertEquals(2, recentCoverage.completionCount)
        assertEquals(events[1].timestamp, recentCoverage.lastCompletedDate)
    }

    @Test
    fun `ordenacoes partem sempre da lista completa`() {
        val neverWorked = CoverageCalculator.sort(coverages, CoverageSortOption.NEVER_WORKED)
        assertEquals(listOf("never"), neverWorked.ids())

        // Antes da correção, ordenar após o filtro acima mantinha apenas "never"
        assertEquals(listOf("never", "old", "recent"), CoverageCalculator.sort(coverages, CoverageSortOption.OLDEST_COMPLETED_FIRST).ids())
        assertEquals(listOf("recent", "old", "never"), CoverageCalculator.sort(coverages, CoverageSortOption.NEWEST_COMPLETED_FIRST).ids())
        assertEquals("recent", CoverageCalculator.sort(coverages, CoverageSortOption.MOST_WORKED).first().territory.id)
    }

    private fun List<TerritoryCoverage>.ids() = map { it.territory.id }

    private fun completed(territoryId: String, daysAgo: Long) = TerritoryEvent(
        id = "$territoryId-$daysAgo",
        territoryId = territoryId,
        type = EventType.COMPLETED,
        timestamp = Fixtures.daysAgo(daysAgo)
    )
}
