//package com.mraphaelpy.terriflow.domain.usecase
//
//import com.mraphaelpy.terriflow.domain.model.Territory
//import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
//import com.mraphaelpy.terriflow.domain.model.User
//import com.mraphaelpy.terriflow.domain.model.UserRole
//import com.mraphaelpy.terriflow.domain.repository.AuthRepository
//import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
//import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
//import com.mraphaelpy.terriflow.domain.usecase.territory.CreateTerritoryUseCase
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.test.runTest
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import java.util.Date
//
//class CreateTerritoryUseCaseTest {
//
//    private lateinit var useCase: CreateTerritoryUseCase
//    private val territoryRepository: TerritoryRepository = mockk()
//    private val eventRepository: TerritoryEventRepository = mockk(relaxed = true)
//    private val authRepository: AuthRepository = mockk()
//
//    private val testUser = User(
//        id = "user1",
//        name = "Admin",
//        email = "admin@test.com",
//        role = UserRole.ADMIN,
//        createdAt = Date()
//    )
//
//    @Before
//    fun setup() {
//        useCase = CreateTerritoryUseCase(territoryRepository, eventRepository, authRepository)
//    }
//
//    @Test
//    fun `deve criar território com código único e status disponível`() = runTest {
//        coEvery { authRepository.getCurrentUser() } returns testUser
//        coEvery { territoryRepository.getNextCode() } returns "T-00001"
//        coEvery { territoryRepository.save(any()) } answers {
//            firstArg<Territory>().copy(id = "terr1")
//        }
//
//        val result = useCase("Centro", "Bairro central", "Rua A", "")
//
//        assertEquals("T-00001", result.code)
//        assertEquals("Centro", result.name)
//        assertEquals(TerritoryStatus.AVAILABLE, result.status)
//        coVerify { eventRepository.save(any()) }
//    }
//
//    @Test
//    fun `deve lançar exceção quando usuário não está autenticado`() = runTest {
//        coEvery { authRepository.getCurrentUser() } returns null
//
//        assertThrows(IllegalStateException::class.java) {
//            runTest { useCase("Centro", "", "", "") }
//        }
//    }
//}
