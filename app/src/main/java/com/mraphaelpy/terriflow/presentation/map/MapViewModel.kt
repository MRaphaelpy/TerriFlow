package com.mraphaelpy.terriflow.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val territories: List<Territory> = emptyList(),
    val territoriesWithLocation: List<Territory> = emptyList(),
    val currentUserId: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val territoryRepository: TerritoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState = combine(
        territoryRepository.observeAll(),
        authRepository.observeCurrentUser()
    ) { all, user ->
        MapUiState(
            territories = all,
            territoriesWithLocation = all.filter { it.hasLocation || it.hasBoundary || it.hasBlocks },
            currentUserId = user?.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapUiState())

    init {
        viewModelScope.launch {
            runCatching { territoryRepository.syncFromRemote() }
        }
    }
}
