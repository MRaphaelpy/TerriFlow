package com.mraphaelpy.terriflow.domain.usecase.notification

import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.testutil.Fixtures
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
        coEvery { notificationRepository.exists(any()) } returns false
    }

    private fun mine(status: TerritoryStatus, assignedDaysAgo: Long, started: Boolean = status != TerritoryStatus.ASSIGNED) =
        Fixtures.territory(
            status,
            publisher,
            assignedAt = Fixtures.daysAgo(assignedDaysAgo, now),
            startedAt = if (started) Fixtures.daysAgo(assignedDaysAgo - 1, now) else null
        )

    @Test
    fun `lembra aos 2 e aos 4 meses com ids diferentes`() = runTest {
        coEvery { territoryRepository.getByResponsible(publisher.id) } returns listOf(mine(TerritoryStatus.IN_PROGRESS, 65))
        val twoMonths = useCase(now).single()
        assertTrue(twoMonths.title.contains("2 meses"))

        coEvery { territoryRepository.getByResponsible(publisher.id) } returns listOf(mine(TerritoryStatus.IN_PROGRESS, 125))
        val fourMonths = useCase(now).single()
        assertTrue(fourMonths.title.contains("4 meses"))
        assertTrue(twoMonths.id != fourMonths.id)
    }

    @Test
    fun `avisa territorio designado e nao iniciado`() = runTest {
        coEvery { territoryRepository.getByResponsible(publisher.id) } returns listOf(mine(TerritoryStatus.ASSIGNED, 8))
        assertEquals("Território aguardando início", useCase(now).single().title)
    }

    @Test
    fun `nao repete lembrete ja enviado nem avisa dentro do prazo`() = runTest {
        coEvery { territoryRepository.getByResponsible(publisher.id) } returns listOf(mine(TerritoryStatus.IN_PROGRESS, 30))
        assertTrue(useCase(now).isEmpty())

        coEvery { territoryRepository.getByResponsible(publisher.id) } returns listOf(mine(TerritoryStatus.IN_PROGRESS, 70))
        coEvery { notificationRepository.exists(any()) } returns true
        assertTrue(useCase(now).isEmpty())
        coVerify(exactly = 0) { notificationRepository.save(any()) }
    }
}
