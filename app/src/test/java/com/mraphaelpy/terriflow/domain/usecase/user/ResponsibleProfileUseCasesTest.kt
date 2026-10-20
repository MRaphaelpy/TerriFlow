package com.mraphaelpy.terriflow.domain.usecase.user

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import com.mraphaelpy.terriflow.testutil.Fixtures.admin
import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResponsibleProfileUseCasesTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val territoryRepository: TerritoryRepository = mockk(relaxed = true)

    @Test
    fun `renomear a si mesmo atualiza o perfil e os territorios`() = runTest {
        every { authRepository.currentUserId } returns publisher.id
        val renamed = publisher.copy(name = "Novo Nome")
        coEvery { userRepository.getById(publisher.id) } returns renamed

        RenameUserUseCase(authRepository, userRepository, territoryRepository)(publisher.id, "  Novo Nome ")

        coVerify { authRepository.updateName("Novo Nome") }
        coVerify { territoryRepository.updateResponsibleName(renamed) }
    }

    @Test
    fun `admin renomeando outro usuario atualiza os territorios dele`() = runTest {
        every { authRepository.currentUserId } returns admin.id
        coEvery { userRepository.getById(publisher.id) } returns publisher

        RenameUserUseCase(authRepository, userRepository, territoryRepository)(publisher.id, "Outro")

        coVerify { userRepository.updateUserName(publisher.id, "Outro") }
        coVerify(exactly = 0) { authRepository.updateName(any()) }
        coVerify { territoryRepository.updateResponsibleName(publisher) }
    }

    @Test
    fun `reconciliacao do admin cobre todos os usuarios`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns admin
        every { userRepository.observeAll() } returns flowOf(listOf(admin, publisher, otherPublisher))

        SyncResponsibleProfilesUseCase(authRepository, userRepository, territoryRepository)()

        coVerify(exactly = 3) { territoryRepository.updateResponsibleName(any()) }
    }

    @Test
    fun `reconciliacao do publicador cobre apenas os proprios territorios`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns publisher

        SyncResponsibleProfilesUseCase(authRepository, userRepository, territoryRepository)()

        coVerify(exactly = 1) { territoryRepository.updateResponsibleName(publisher) }
    }
}
