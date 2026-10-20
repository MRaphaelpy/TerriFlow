package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import com.mraphaelpy.terriflow.domain.usecase.notification.NotifyAdminsAndOwnerUseCase
import com.mraphaelpy.terriflow.testutil.Fixtures
import com.mraphaelpy.terriflow.testutil.Fixtures.admin
import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TerritoryWorkflowUseCasesTest {

    private val authRepository: AuthRepository = mockk()
    private val territoryRepository: TerritoryRepository = mockk(relaxed = true)
    private val eventRepository: TerritoryEventRepository = mockk(relaxed = true)
    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk()
    private val guard = TerritoryActionGuard(authRepository, territoryRepository)
    private val notifyAdminsAndOwner = NotifyAdminsAndOwnerUseCase(userRepository, notificationRepository)

    init {
        coEvery { userRepository.getActiveAdmins() } returns listOf(admin)
    }

    private fun given(user: User?, territory: Territory) {
        coEvery { authRepository.getCurrentUser() } returns user
        coEvery { territoryRepository.getById(territory.id) } returns territory
    }

    @Test
    fun `devolver grava o evento antes do territorio e limpa o responsavel`() = runTest {
        val territory = Fixtures.territory(TerritoryStatus.IN_PROGRESS, responsible = publisher)
        given(publisher, territory)
        val saved = slot<Territory>()
        coEvery { territoryRepository.save(capture(saved)) } answers { saved.captured }

        ReturnTerritoryUseCase(guard, territoryRepository, eventRepository, notifyAdminsAndOwner)(territory.id)

        coVerifyOrder {
            eventRepository.save(match { it.type == EventType.RETURNED })
            territoryRepository.save(any())
        }
        assertEquals(TerritoryStatus.RETURNED, saved.captured.status)
        assertNull(saved.captured.currentResponsibleId)
        assertTrue(publisher.id in saved.captured.pastResponsibleIds)
        // Só o admin é avisado: o responsável foi quem devolveu
        coVerify(exactly = 1) { notificationRepository.save(any()) }
        coVerify { notificationRepository.save(match { it.userId == admin.id }) }
    }

    @Test
    fun `finalizacao feita por admin avisa o responsavel e nao o proprio admin`() = runTest {
        val territory = Fixtures.territory(TerritoryStatus.IN_PROGRESS, responsible = publisher)
        given(admin, territory)
        coEvery { territoryRepository.save(any()) } answers { firstArg() }

        CompleteTerritoryUseCase(guard, territoryRepository, eventRepository, notificationRepository, userRepository, notifyAdminsAndOwner)(
            territory.id
        )

        coVerify(exactly = 1) { notificationRepository.save(any()) }
        coVerify { notificationRepository.save(match { it.userId == publisher.id && it.title == "Território finalizado" }) }
    }

    @Test
    fun `publicador nao consegue devolver territorio de outra pessoa`() = runTest {
        val territory = Fixtures.territory(TerritoryStatus.IN_PROGRESS, responsible = otherPublisher)
        given(publisher, territory)

        try {
            ReturnTerritoryUseCase(guard, territoryRepository, eventRepository, notifyAdminsAndOwner)(territory.id)
            fail("Deveria lançar ActionNotAllowed")
        } catch (expected: DomainException.ActionNotAllowed) {
        }

        coVerify(exactly = 0) { territoryRepository.save(any()) }
        coVerify(exactly = 0) { eventRepository.save(any()) }
    }

    @Test
    fun `finalizar com proximo responsavel transfere em uma unica gravacao`() = runTest {
        val territory = Fixtures.territory(TerritoryStatus.IN_PROGRESS, responsible = publisher)
        given(publisher, territory)
        coEvery { userRepository.getById(otherPublisher.id) } returns otherPublisher
        val saved = mutableListOf<Territory>()
        coEvery { territoryRepository.save(capture(saved)) } answers { saved.last() }
        val events = mutableListOf<TerritoryEvent>()
        coEvery { eventRepository.save(capture(events)) } returns Unit

        CompleteTerritoryUseCase(guard, territoryRepository, eventRepository, notificationRepository, userRepository, notifyAdminsAndOwner)(
            territory.id, nextResponsibleId = otherPublisher.id
        )

        assertEquals(listOf(EventType.COMPLETED, EventType.TRANSFERRED), events.map { it.type })
        assertEquals(1, saved.size)
        with(saved.single()) {
            assertEquals(TerritoryStatus.ASSIGNED, status)
            assertEquals(otherPublisher.id, currentResponsibleId)
            assertNull(startedAt)
            assertTrue(pastResponsibleIds.containsAll(listOf(publisher.id, otherPublisher.id)))
        }
        coVerifyOrder {
            eventRepository.save(any())
            eventRepository.save(any())
            territoryRepository.save(any())
        }
        coVerify { notificationRepository.save(match { it.userId == otherPublisher.id }) }
        coVerify { notificationRepository.save(match { it.userId == admin.id && it.title == "Território finalizado" }) }
    }

    @Test
    fun `atribuir limpa as datas do ciclo anterior`() = runTest {
        val territory = Fixtures.territory(TerritoryStatus.COMPLETED, startedAt = Fixtures.daysAgo(30))
        given(admin, territory)
        coEvery { userRepository.getById(publisher.id) } returns publisher
        val saved = slot<Territory>()
        coEvery { territoryRepository.save(capture(saved)) } answers { saved.captured }

        AssignTerritoryUseCase(guard, territoryRepository, eventRepository, notificationRepository, userRepository)(
            territory.id, publisher.id
        )

        assertEquals(TerritoryStatus.ASSIGNED, saved.captured.status)
        assertEquals(publisher.id, saved.captured.currentResponsibleId)
        assertNull(saved.captured.startedAt)
        assertNull(saved.captured.completedAt)
    }

    @Test
    fun `iniciar territorio pausado registra RESUMED e preserva a data de inicio`() = runTest {
        val startedAt = Fixtures.daysAgo(3)
        val territory = Fixtures.territory(TerritoryStatus.PAUSED, responsible = publisher, startedAt = startedAt)
        given(publisher, territory)
        val saved = slot<Territory>()
        coEvery { territoryRepository.save(capture(saved)) } answers { saved.captured }

        StartTerritoryUseCase(guard, territoryRepository, eventRepository)(territory.id)

        coVerify { eventRepository.save(match { it.type == EventType.RESUMED }) }
        assertEquals(TerritoryStatus.IN_PROGRESS, saved.captured.status)
        assertEquals(startedAt, saved.captured.startedAt)
    }
}
