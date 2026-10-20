package com.mraphaelpy.terriflow.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.AuthException
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val needsCongregationSetup: Boolean = false,
    val error: String? = null,
    val resetEmailSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = authRepository.isLoggedIn))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Preencha e-mail e senha")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { authRepository.login(email.trim(), password) }
                .onSuccess {
                    val hasCongregation = authRepository.resolveAndSaveCongregationId() != null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = hasCongregation,
                        needsCongregationSetup = !hasCongregation
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it)) }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Preencha todos os campos")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "A senha deve ter pelo menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { authRepository.register(name.trim(), email.trim(), password) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, needsCongregationSetup = true)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it)) }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { authRepository.signInWithGoogle(idToken) }
                .onSuccess {
                    val hasCongregation = authRepository.resolveAndSaveCongregationId() != null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = hasCongregation,
                        needsCongregationSetup = !hasCongregation
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it)) }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Informe o e-mail")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { authRepository.sendPasswordReset(email.trim()) }
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, resetEmailSent = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(it)) }
        }
    }

    fun onGoogleSignInFailed(statusCode: Int?) {
        if (statusCode == GOOGLE_SIGN_IN_CANCELLED) return
        _uiState.value = _uiState.value.copy(
            error = "Não foi possível entrar com o Google" + (statusCode?.let { " (código $it)" } ?: "")
        )
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun friendlyError(e: Throwable): String = when (e) {
        is AuthException -> e.message.orEmpty()
        else -> AuthException.Unknown(e).message.orEmpty()
    }

    private companion object {
        const val GOOGLE_SIGN_IN_CANCELLED = 12501
    }
}
