package com.mraphaelpy.terriflow.domain.model

import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import org.junit.Assert.assertEquals
import org.junit.Test

class EventNamesTest {

    @Test
    fun `troca nomes gravados pelos nomes atuais dos usuarios`() {
        val event = TerritoryEvent(
            userId = publisher.id,
            userName = "Nome Antigo",
            type = EventType.TRANSFERRED,
            extra = mapOf(
                "fromUserId" to publisher.id, "fromUserName" to "Nome Antigo",
                "toUserId" to otherPublisher.id, "toUserName" to "Outro Antigo"
            )
        )

        val updated = listOf(event).withCurrentNames(listOf(publisher, otherPublisher)).single()

        assertEquals(publisher.name, updated.userName)
        assertEquals(publisher.name, updated.extra["fromUserName"])
        assertEquals(otherPublisher.name, updated.extra["toUserName"])
    }

    @Test
    fun `mantem o nome gravado quando o usuario nao existe mais`() {
        val event = TerritoryEvent(userId = "removido", userName = "Ex-membro")
        assertEquals("Ex-membro", listOf(event).withCurrentNames(listOf(publisher)).single().userName)
    }
}
