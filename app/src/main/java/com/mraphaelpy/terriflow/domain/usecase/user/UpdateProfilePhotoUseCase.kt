package com.mraphaelpy.terriflow.domain.usecase.user

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import javax.inject.Inject

class UpdateProfilePhotoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository
) {
    suspend operator fun invoke(photoUrl: String) {
        authRepository.updatePhotoUrl(photoUrl)
        authRepository.getCurrentUser()?.let { territoryRepository.updateResponsibleProfile(it) }
    }
}
