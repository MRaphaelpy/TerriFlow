package com.mraphaelpy.terriflow.presentation.territories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val createdTerritoryId: String? = null
)

@HiltViewModel
class CreateTerritoryViewModel @Inject constructor(
    private val createTerritoryUseCase: CreateTerritoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTerritoryUiState())
    val uiState: StateFlow<CreateTerritoryUiState> = _uiState.asStateFlow()

    fun createTerritory(code: String, name: String, description: String, location: String, notes: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "O nome é obrigatório")
            return
        }
        viewModelScope.launch {
            _uiState.value = CreateTerritoryUiState(isLoading = true)
            runCatching { createTerritoryUseCase(code.ifBlank { null }, name.trim(), description.trim(), location.trim(), notes.trim()) }
                .onSuccess { territory ->
                    _uiState.value = CreateTerritoryUiState(success = true, createdTerritoryId = territory.id)
                }
                .onFailure { e ->
                    _uiState.value = CreateTerritoryUiState(error = e.message)
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
