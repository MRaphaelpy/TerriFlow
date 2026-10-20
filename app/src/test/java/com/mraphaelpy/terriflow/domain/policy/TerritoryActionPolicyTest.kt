package com.mraphaelpy.terriflow.domain.policy

import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.testutil.Fixtures.admin
import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import com.mraphaelpy.terriflow.testutil.Fixtures.territory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerritoryActionPolicyTest {

    @Test
    fun `admin pode atribuir territorio disponivel, devolvido ou finalizado`() {
        listOf(TerritoryStatus.AVAILABLE, TerritoryStatus.RETURNED, TerritoryStatus.COMPLETED).forEach { status ->
            assertTrue(TerritoryActionPolicy.canPerform(admin, territory(status), TerritoryAction.ASSIGN))
        }
        assertFalse(TerritoryActionPolicy.canPerform(admin, territory(TerritoryStatus.IN_PROGRESS), TerritoryAction.ASSIGN))
    }

    @Test
    fun `publicador nao pode atribuir, editar nem apagar`() {
        val t = territory(TerritoryStatus.AVAILABLE)
        listOf(TerritoryAction.ASSIGN, TerritoryAction.EDIT, TerritoryAction.DELETE).forEach { action ->
            assertFalse(TerritoryActionPolicy.canPerform(publisher, t, action))
        }
    }

    @Test
    fun `responsavel opera o proprio territorio conforme o status`() {
        val assigned = territory(TerritoryStatus.ASSIGNED, responsible = publisher)
        assertEquals(
            setOf(TerritoryAction.START, TerritoryAction.RETURN),
            TerritoryActionPolicy.allowedActions(publisher, assigned)
        )

        val inProgress = territory(TerritoryStatus.IN_PROGRESS, responsible = publisher)
        assertEquals(
            setOf(TerritoryAction.PAUSE, TerritoryAction.COMPLETE, TerritoryAction.RETURN),
            TerritoryActionPolicy.allowedActions(publisher, inProgress)
        )
    }

    @Test
    fun `publicador nao opera territorio de outra pessoa`() {
        val t = territory(TerritoryStatus.IN_PROGRESS, responsible = otherPublisher)
        assertTrue(TerritoryActionPolicy.allowedActions(publisher, t).isEmpty())
    }

    @Test
    fun `usuario inativo ou ausente nao pode nada`() {
        val t = territory(TerritoryStatus.ASSIGNED, responsible = publisher)
        assertTrue(TerritoryActionPolicy.allowedActions(publisher.copy(active = false), t).isEmpty())
        assertTrue(TerritoryActionPolicy.allowedActions(null, t).isEmpty())
        assertFalse(TerritoryActionPolicy.canCreate(admin.copy(active = false)))
    }

    @Test
    fun `apenas admin pode criar`() {
        assertTrue(TerritoryActionPolicy.canCreate(admin))
        assertFalse(TerritoryActionPolicy.canCreate(publisher))
    }
}
