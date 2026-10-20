package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.ImportedTerritory
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.testutil.Fixtures
import com.mraphaelpy.terriflow.testutil.Fixtures.admin
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateTerritoryUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val territoryRepository: TerritoryRepository = mockk(relaxed = true)
    private val eventRepository: TerritoryEventRepository = mockk(relaxed = true)
    private val guard = TerritoryActionGuard(authRepository, territoryRepository)

    private val create = CreateTerritoryUseCase(guard, territoryRepository, eventRepository)
    private val import = ImportTerritoriesUseCase(guard, territoryRepository, eventRepository)

    init {
        coEvery { territoryRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `gera codigo automatico quando nenhum e informado`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns admin
        coEvery { territoryRepository.getNextCode() } returns "T-00005"

        val result = create(code = " ", name = " Centro ", description = "", location = "", notes = "")

        assertEquals("T-00005", result.code)
        assertEquals("Centro", result.name)
        assertEquals(TerritoryStatus.AVAILABLE, result.status)
        coVerify { eventRepository.save(any()) }
    }

    @Test(expected = DomainException.DuplicateTerritoryCode::class)
    fun `rejeita codigo ja existente`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns admin
        coEvery { territoryRepository.getByCode("T-00001") } returns Fixtures.territory()

        create(code = "T-00001", name = "Centro", description = "", location = "", notes = "")
    }

    @Test(expected = DomainException.ActionNotAllowed::class)
    fun `publicador nao pode criar territorio`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns publisher

        create(code = null, name = "Centro", description = "", location = "", notes = "")
    }

    @Test(expected = DomainException.NotAuthenticated::class)
    fun `exige usuario autenticado`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns null

        create(code = null, name = "Centro", description = "", location = "", notes = "")
    }

    @Test
    fun `importacao usa codigos sequenciais com uma unica consulta`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns admin
        coEvery { territoryRepository.getNextCode() } returns "T-00010"
        val saved = mutableListOf<Territory>()
        coEvery { territoryRepository.save(capture(saved)) } answers { saved.last() }
        val items = List(3) { ImportedTerritory(name = "", description = "", boundaryPoints = emptyList(), centerPoint = null) }

        val count = import(items)

        assertEquals(3, count)
        assertEquals(listOf("T-00010", "T-00011", "T-00012"), saved.map { it.code })
        assertEquals("Território T-00010", saved.first().name)
        coVerify(exactly = 1) { territoryRepository.getNextCode() }
    }
}
