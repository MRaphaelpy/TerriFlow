package com.mraphaelpy.terriflow.domain.usecase.user

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncResponsibleProfilesUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val territoryRepository: TerritoryRepository
) {
    suspend operator fun invoke(): Int {
        val me = authRepository.getCurrentUser() ?: return 0
        val users = if (me.isAdmin) userRepository.observeAll().first() else listOf(me)
        return users.sumOf { territoryRepository.updateResponsibleProfile(it) }
    }
}
