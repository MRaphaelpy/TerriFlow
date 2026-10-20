package com.mraphaelpy.terriflow.presentation.territories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.usecase.territory.CreateTerritoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateTerritoryUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val createdTerritoryId: String? = null,
    val nextCode: String? = null,
    val nameError: Boolean = false
)

@HiltViewModel
class CreateTerritoryViewModel @Inject constructor(
    private val createTerritoryUseCase: CreateTerritoryUseCase,
    territoryRepository: TerritoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTerritoryUiState())
    val uiState: StateFlow<CreateTerritoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val next = runCatching { territoryRepository.getNextCode() }.getOrNull()
            _uiState.value = _uiState.value.copy(nextCode = next)
        }
    }

    fun createTerritory(name: String, description: String, location: String, notes: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "O nome é obrigatório", nameError = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, nameError = false)
            runCatching { createTerritoryUseCase(name.trim(), description.trim(), location.trim(), notes.trim()) }
                .onSuccess { territory ->
                    _uiState.value = _uiState.value.copy(isLoading = false, success = true, createdTerritoryId = territory.id)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
