package com.mraphaelpy.terriflow.presentation.territories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.LatLng
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.usecase.territory.UpdateTerritoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditTerritoryUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val boundaryPoints: List<LatLng> = emptyList(),
    val code: String = ""
)

@HiltViewModel
class EditTerritoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val territoryRepository: TerritoryRepository,
    private val updateTerritoryUseCase: UpdateTerritoryUseCase
) : ViewModel() {

    private val territoryId: String = checkNotNull(savedStateHandle["territoryId"])

    private val _uiState = MutableStateFlow(EditTerritoryUiState())
    val uiState: StateFlow<EditTerritoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val t = territoryRepository.getById(territoryId)
            _uiState.value = if (t != null) {
                EditTerritoryUiState(
                    isLoading = false,
                    name = t.name,
                    description = t.description,
                    location = t.location,
                    notes = t.notes,
                    latitude = t.latitude,
                    longitude = t.longitude,
                    boundaryPoints = t.boundaryPoints,
                    code = t.code
                )
            } else {
                EditTerritoryUiState(isLoading = false, error = "Território não encontrado")
            }
        }
    }

    fun updatePin(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(latitude = lat, longitude = lng)
    }

    fun clearPin() {
        _uiState.value = _uiState.value.copy(latitude = null, longitude = null)
    }

    fun addBoundaryPoint(point: LatLng) {
        val current = _uiState.value.boundaryPoints.toMutableList()
        current.add(point)
        _uiState.value = _uiState.value.copy(boundaryPoints = current)
    }

    fun removeLastBoundaryPoint() {
        val current = _uiState.value.boundaryPoints.toMutableList()
        if (current.isNotEmpty()) current.removeAt(current.lastIndex)
        _uiState.value = _uiState.value.copy(boundaryPoints = current)
    }

    fun updateBoundaryPoint(index: Int, point: LatLng) {
        val current = _uiState.value.boundaryPoints.toMutableList()
        if (index in current.indices) {
            current[index] = point
            _uiState.value = _uiState.value.copy(boundaryPoints = current)
        }
    }

    fun clearBoundary() {
        _uiState.value = _uiState.value.copy(boundaryPoints = emptyList())
    }

    fun setBoundaryPoints(points: List<LatLng>) {
        _uiState.value = _uiState.value.copy(boundaryPoints = points)
    }

    fun save(code: String, name: String, description: String, location: String, notes: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "O nome é obrigatório")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            runCatching {
                updateTerritoryUseCase(
                    id = territoryId,
                    code = code.ifBlank { null },
                    name = name,
                    description = description,
                    location = location,
                    notes = notes,
                    latitude = _uiState.value.latitude,
                    longitude = _uiState.value.longitude,
                    boundaryPoints = _uiState.value.boundaryPoints
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, success = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
