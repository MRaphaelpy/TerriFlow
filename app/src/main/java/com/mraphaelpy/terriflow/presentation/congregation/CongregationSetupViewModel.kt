package com.mraphaelpy.terriflow.presentation.congregation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.usecase.congregation.CreateCongregationUseCase
import com.mraphaelpy.terriflow.domain.usecase.congregation.JoinCongregationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CongregationSetupUiState(
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val congregation: Congregation? = null,
    val error: String? = null
)

@HiltViewModel
class CongregationSetupViewModel @Inject constructor(
    private val createCongregation: CreateCongregationUseCase,
    private val joinCongregation: JoinCongregationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CongregationSetupUiState())
    val uiState: StateFlow<CongregationSetupUiState> = _uiState.asStateFlow()

    fun create(name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Informe o nome da congregação") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { createCongregation(name) }
                .onSuccess { congregation ->
                    _uiState.update { it.copy(isLoading = false, congregation = congregation) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erro ao criar congregação") }
                }
        }
    }

    fun joinByCode(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(error = "Informe o código") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { joinCongregation(code) }
                .onSuccess { _uiState.update { it.copy(isLoading = false, isComplete = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Código inválido") }
                }
        }
    }
}
