package com.mraphaelpy.terriflow.domain.usecase.user

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import javax.inject.Inject

class RenameUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val territoryRepository: TerritoryRepository
) {
    suspend operator fun invoke(userId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return

        if (userId == authRepository.currentUserId) {
            authRepository.updateName(trimmed)
        } else {
            userRepository.updateUserName(userId, trimmed)
        }
        userRepository.getById(userId)?.let { territoryRepository.updateResponsibleProfile(it) }
    }
}
