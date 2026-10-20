package com.mraphaelpy.terriflow.domain.usecase.notification

import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.testutil.Fixtures
import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CheckStaleTerritoriesUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val territoryRepository: TerritoryRepository = mockk()
    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val useCase = CheckStaleTerritoriesUseCase(authRepository, territoryRepository, notificationRepository)
    private val now = Date()

    init {
        every { authRepository.currentUserId } returns publisher.id
        coEvery { territoryRepository.getInProgress() } returns emptyList()
    }

    @Test
    fun `avisa apenas sobre os territorios do proprio usuario`() = runTest {
        coEvery { territoryRepository.getAssignedNotStarted() } returns listOf(
            Fixtures.territory(TerritoryStatus.ASSIGNED, publisher, assignedAt = Fixtures.daysAgo(10, now)),
            Fixtures.territory(TerritoryStatus.ASSIGNED, otherPublisher, assignedAt = Fixtures.daysAgo(10, now))
                .copy(id = "t2")
        )
        coEvery { notificationRepository.exists(any()) } returns false

        val created = useCase(now)

        assertEquals(listOf("t1"), created.map { it.territoryId })
        assertEquals(publisher.id, created.single().userId)
    }

    @Test
    fun `nao repete o lembrete que ja existe`() = runTest {
        coEvery { territoryRepository.getAssignedNotStarted() } returns listOf(
            Fixtures.territory(TerritoryStatus.ASSIGNED, publisher, assignedAt = Fixtures.daysAgo(10, now))
        )
        coEvery { notificationRepository.exists(any()) } returns true

        assertTrue(useCase(now).isEmpty())
        coVerify(exactly = 0) { notificationRepository.save(any()) }
    }

    @Test
    fun `gera o mesmo id dentro da mesma semana`() = runTest {
        val territory = Fixtures.territory(TerritoryStatus.ASSIGNED, publisher, assignedAt = Fixtures.daysAgo(8, now))
        coEvery { territoryRepository.getAssignedNotStarted() } returns listOf(territory)
        coEvery { notificationRepository.exists(any()) } returns false

        val first = useCase(now).single().id
        val sameWeek = useCase(Date(now.time + 2 * 24 * 60 * 60 * 1000)).single().id
        val nextWeek = useCase(Date(now.time + 7 * 24 * 60 * 60 * 1000)).single().id

        assertEquals(first, sameWeek)
        assertTrue(first != nextWeek)
    }

    @Test
    fun `ignora territorios dentro do prazo`() = runTest {
        coEvery { territoryRepository.getAssignedNotStarted() } returns listOf(
            Fixtures.territory(TerritoryStatus.ASSIGNED, publisher, assignedAt = Fixtures.daysAgo(2, now))
        )

        assertTrue(useCase(now).isEmpty())
    }
}
