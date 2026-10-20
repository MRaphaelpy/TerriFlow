package com.mraphaelpy.terriflow.presentation.territorydetail

import com.mraphaelpy.terriflow.domain.model.TerritoryAction.ASSIGN
import com.mraphaelpy.terriflow.domain.model.TerritoryAction.COMPLETE
import com.mraphaelpy.terriflow.domain.model.TerritoryAction.PAUSE
import com.mraphaelpy.terriflow.domain.model.TerritoryAction.RETURN
import com.mraphaelpy.terriflow.domain.model.TerritoryAction.START
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class TerritoryDetailFormattingTest {

    @Test
    fun `em andamento destaca finalizar e deixa pausar e devolver como secundarias`() {
        assertEquals(ActionLayout(COMPLETE, listOf(PAUSE, RETURN)), actionLayout(setOf(PAUSE, COMPLETE, RETURN)))
    }

    @Test
    fun `pausado destaca retomar`() {
        assertEquals(ActionLayout(START, listOf(COMPLETE, RETURN)), actionLayout(setOf(START, COMPLETE, RETURN)))
    }

    @Test
    fun `disponivel para admin destaca atribuir`() {
        assertEquals(ActionLayout(ASSIGN, emptyList()), actionLayout(setOf(ASSIGN)))
        assertEquals(ActionLayout(null, emptyList()), actionLayout(emptySet()))
    }

    @Test
    fun `descreve ha quanto tempo esta no status`() {
        val now = Date()
        val inProgress = Fixtures.territory(TerritoryStatus.IN_PROGRESS, startedAt = Fixtures.daysAgo(3, now))
        assertEquals("Iniciado há 3 dias", statusSince(inProgress, now))
        assertEquals("ontem", relativeDays(Fixtures.daysAgo(1, now), now))
        assertEquals("hoje", relativeDays(now, now))
    }
}
