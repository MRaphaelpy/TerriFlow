package com.mraphaelpy.terriflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerritoryCodeTest {

    @Test
    fun `formata com cinco digitos`() {
        assertEquals("T-00042", TerritoryCode.format(42))
    }

    @Test
    fun `ignora codigos personalizados ao calcular o proximo`() {
        assertEquals("T-00011", TerritoryCode.nextAfter(listOf("T-00003", "Centro", "T-00010", "ZZZ")))
    }

    @Test
    fun `comeca do um quando nao ha codigos automaticos`() {
        assertEquals("T-00001", TerritoryCode.nextAfter(listOf("Centro")))
        assertEquals("T-00001", TerritoryCode.nextAfter(emptyList()))
    }

    @Test
    fun `numberOf retorna null para codigo fora do padrao`() {
        assertNull(TerritoryCode.numberOf("T-ABC"))
        assertEquals(7, TerritoryCode.numberOf(" T-00007 "))
    }
}
