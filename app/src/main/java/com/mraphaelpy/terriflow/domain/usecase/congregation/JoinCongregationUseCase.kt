package com.mraphaelpy.terriflow.domain.usecase.congregation

import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import javax.inject.Inject

class JoinCongregationUseCase @Inject constructor(
    private val congregationRepository: CongregationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): Congregation {
        val userId = authRepository.currentUserId ?: throw DomainException.NotAuthenticated()
        val congregation = congregationRepository.joinByCode(code.trim(), userId)
        authRepository.ensureCongregationProfile(congregation.id, roleIfNew = UserRole.RESPONSIBLE)
        return congregation
    }
}
