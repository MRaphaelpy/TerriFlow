package com.mraphaelpy.terriflow.domain.model

import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class CampaignCoverageTest {

    private val now = Date()

    private fun event(territory: String, type: EventType, daysAgo: Long) = TerritoryEvent(
        id = "$territory-$type-$daysAgo", territoryId = territory, type = type,
        timestamp = Fixtures.daysAgo(daysAgo, now),
        extra = if (type == EventType.ASSIGNED) mapOf("responsibleId" to "p", "responsibleName" to "P") else emptyMap()
    )

    @Test
    fun `classifica territorios na campanha`() {
        val done = Fixtures.territory().copy(id = "done")
        val working = Fixtures.territory().copy(id = "working")
        val idle = Fixtures.territory().copy(id = "idle")
        val oldWork = Fixtures.territory().copy(id = "old")
        val events = listOf(
            event("done", EventType.ASSIGNED, 20), event("done", EventType.COMPLETED, 8),
            event("working", EventType.ASSIGNED, 12),
            event("old", EventType.ASSIGNED, 100), event("old", EventType.COMPLETED, 90)
        )
        val campaign = Campaign(name = "Celebração", start = Fixtures.daysAgo(10, now), end = now)

        val coverage = TerritoryStatistics.campaignCoverage(
            listOf(done, working, idle, oldWork), AssignmentHistory.build(events), campaign
        )

        assertEquals(listOf(done), coverage.completed)
        assertEquals(listOf(working), coverage.inProgress)
        assertEquals(listOf(idle, oldWork), coverage.notWorked)
        assertEquals(25, coverage.percent)
    }
}
