package com.mraphaelpy.terriflow.domain.model

import com.mraphaelpy.terriflow.testutil.Fixtures
import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationInfoTest {

    private val third = User(id = "pub3", name = "Terceiro")
    private val target = Fixtures.territory().copy(id = "target", pastResponsibleIds = listOf(publisher.id))

    @Test
    fun `recomenda quem nunca trabalhou e esta com menos territorios`() {
        val busy = (1..3).map {
            Fixtures.territory(TerritoryStatus.IN_PROGRESS, otherPublisher).copy(id = "busy$it")
        }
        val infos = TerritoryRotationHelper.computeRotationInfo(
            responsibles = listOf(publisher, otherPublisher, third),
            territory = target,
            events = emptyList(),
            allTerritories = busy + target
        )

        assertEquals(listOf(third.id, otherPublisher.id, publisher.id), infos.map { it.user.id })
        assertTrue(infos.first().isRecommended)
        val overloaded = infos.first { it.user.id == otherPublisher.id }
        assertEquals(3, overloaded.currentTerritories)
        assertTrue(overloaded.isOverLimit)
    }
}
