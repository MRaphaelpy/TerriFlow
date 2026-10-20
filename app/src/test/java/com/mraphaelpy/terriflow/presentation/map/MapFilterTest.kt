package com.mraphaelpy.terriflow.presentation.map

import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.testutil.Fixtures
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFilterTest {

    private val centro = Fixtures.territory().copy(id = "a", code = "T-00001", name = "Centro Histórico")
    private val jardim = Fixtures.territory(TerritoryStatus.IN_PROGRESS, responsible = publisher)
        .copy(id = "b", code = "T-00002", name = "Jardim", location = "Rua São João")
    private val all = listOf(centro, jardim)

    @Test
    fun `sem filtro retorna tudo`() {
        assertEquals(all, MapFilter().apply(all, publisher.id))
        assertFalse(MapFilter().isActive)
    }

    @Test
    fun `filtra por status e por meus territorios`() {
        assertEquals(listOf(jardim), MapFilter(status = TerritoryStatus.IN_PROGRESS).apply(all, null))
        assertEquals(listOf(jardim), MapFilter(onlyMine = true).apply(all, publisher.id))
        assertTrue(MapFilter(onlyMine = true).apply(all, null).isEmpty())
    }

    @Test
    fun `busca ignora acentos e maiusculas em codigo, nome, endereco e responsavel`() {
        assertEquals(listOf(centro), MapFilter(query = "historico").apply(all, null))
        assertEquals(listOf(jardim), MapFilter(query = "SAO JOAO").apply(all, null))
        assertEquals(listOf(jardim), MapFilter(query = "00002").apply(all, null))
        assertEquals(listOf(jardim), MapFilter(query = publisher.name).apply(all, null))
    }
}
