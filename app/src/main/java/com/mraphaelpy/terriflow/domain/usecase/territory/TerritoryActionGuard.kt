package com.mraphaelpy.terriflow.domain.usecase.territory

import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.policy.TerritoryActionPolicy
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import javax.inject.Inject

data class AuthorizedTerritoryAction(val actor: User, val territory: Territory)

class TerritoryActionGuard @Inject constructor(
    private val authRepository: AuthRepository,
    private val territoryRepository: TerritoryRepository
) {
    suspend fun requireCurrentUser(): User =
        authRepository.getCurrentUser() ?: throw DomainException.NotAuthenticated()

    suspend fun requireCanCreate(): User {
        val actor = requireCurrentUser()
        if (!TerritoryActionPolicy.canCreate(actor)) {
            throw DomainException.ActionNotAllowed(TerritoryAction.CREATE)
        }
        return actor
    }

    suspend fun authorize(territoryId: String, action: TerritoryAction): AuthorizedTerritoryAction {
        val actor = requireCurrentUser()
        val territory = territoryRepository.getById(territoryId)
            ?: throw DomainException.TerritoryNotFound()
        if (!TerritoryActionPolicy.canPerform(actor, territory, action)) {
            throw DomainException.ActionNotAllowed(action)
        }
        return AuthorizedTerritoryAction(actor, territory)
    }
}
