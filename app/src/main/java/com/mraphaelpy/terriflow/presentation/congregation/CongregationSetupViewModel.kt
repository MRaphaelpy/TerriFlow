package com.mraphaelpy.terriflow.presentation.congregation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val congregationRepository: CongregationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CongregationSetupUiState())
    val uiState: StateFlow<CongregationSetupUiState> = _uiState.asStateFlow()

    fun create(name: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Informe o nome da congregação")
            return
        }
        val userId = authRepository.currentUserId ?: run {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { congregationRepository.create(name.trim(), userId) }
                .onSuccess { congregation ->
                    // Salva o perfil do usuário no Firestore agora que a congregação existe
                    syncCurrentUserToFirestore(congregation.id, role = UserRole.ADMIN)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        congregation = congregation
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erro ao criar congregação"
                    )
                }
        }
    }

    fun joinByCode(code: String) {
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Informe o código")
            return
        }
        val userId = authRepository.currentUserId ?: run {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { congregationRepository.joinByCode(code.trim(), userId) }
                .onSuccess { congregation ->
                    // Salva o perfil do usuário no Firestore agora que a congregação é conhecida
                    syncCurrentUserToFirestore(congregation.id, role = UserRole.RESPONSIBLE)
                    _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Código inválido"
                    )
                }
        }
    }

    /**
     * Após a congregação ser definida, persiste o perfil completo do usuário
     * no Firestore dentro da congregação. Sem isso, o nome e demais dados
     * ficam apenas no Room local.
     */
    private suspend fun syncCurrentUserToFirestore(congregationId: String, role: UserRole) {
        runCatching {
            val uid = authRepository.currentUserId ?: return
            // Pega os dados do usuário que estão no Room (foram salvos no registro)
            val localUser = userRepository.getById(uid) ?: return
            // Atualiza congregationId e role, depois salva no Firestore
            val updatedUser = localUser.copy(
                congregationId = congregationId,
                role = role
            )
            userRepository.save(updatedUser)
        }
    }
}
