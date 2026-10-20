package com.mraphaelpy.terriflow.domain.usecase.user

import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateProfilePhotoUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(photoUrl: String) {
        authRepository.updatePhotoUrl(photoUrl)
    }
}
