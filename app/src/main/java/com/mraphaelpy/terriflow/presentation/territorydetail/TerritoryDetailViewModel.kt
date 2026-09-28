package com.mraphaelpy.terriflow.presentation.territorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import com.mraphaelpy.terriflow.domain.usecase.territory.AssignTerritoryUseCase
import com.mraphaelpy.terriflow.domain.usecase.territory.CompleteTerritoryUseCase
import com.mraphaelpy.terriflow.domain.usecase.territory.PauseTerritoryUseCase
import com.mraphaelpy.terriflow.domain.usecase.territory.ReturnTerritoryUseCase
import com.mraphaelpy.terriflow.domain.usecase.territory.StartTerritoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryCycle(
    val responsibleName: String,
    val events: List<TerritoryEvent>
)

data class TerritoryDetailUiState(
    val territory: Territory? = null,
    val events: List<TerritoryEvent> = emptyList(),
    val responsibles: List<User> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionSuccess: String? = null,
    val rotationInfo: List<com.mraphaelpy.terriflow.domain.model.ResponsibleRotationInfo> = emptyList(),
    val pastWorkers: List<com.mraphaelpy.terriflow.domain.model.ResponsibleRotationInfo> = emptyList(),
    val suggestedWorkers: List<com.mraphaelpy.terriflow.domain.model.ResponsibleRotationInfo> = emptyList(),
    val historyCycles: List<HistoryCycle> = emptyList()
) {
    val isAdmin: Boolean get() = currentUser?.role?.name == "ADMIN" || currentUser?.role?.name == "SUPER_ADMIN"
}

@HiltViewModel
class TerritoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val assignTerritory: AssignTerritoryUseCase,
    private val startTerritory: StartTerritoryUseCase,
    private val pauseTerritory: PauseTerritoryUseCase,
    private val completeTerritory: CompleteTerritoryUseCase,
    private val returnTerritory: ReturnTerritoryUseCase
) : ViewModel() {

    private val territoryId: String = checkNotNull(savedStateHandle["territoryId"])

    private val _uiState = MutableStateFlow(TerritoryDetailUiState())
    val uiState: StateFlow<TerritoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.update { it.copy(currentUser = user) }
        }
        viewModelScope.launch {
            territoryRepository.observeById(territoryId).collect { territory ->
                updateStateWithCalculations(territory = territory, isLoading = false)
            }
        }
        viewModelScope.launch {
            eventRepository.observeByTerritory(territoryId).collect { events ->
                updateStateWithCalculations(events = events)
            }
        }
        viewModelScope.launch {
            userRepository.observeResponsibles().collect { responsibles ->
                updateStateWithCalculations(responsibles = responsibles)
            }
        }
        viewModelScope.launch {
            runCatching { eventRepository.syncFromRemote(territoryId) }
        }
    }

    private fun updateStateWithCalculations(
        territory: Territory? = _uiState.value.territory,
        events: List<TerritoryEvent> = _uiState.value.events,
        responsibles: List<User> = _uiState.value.responsibles,
        isLoading: Boolean = _uiState.value.isLoading
    ) {
        val rotation = com.mraphaelpy.terriflow.domain.model.TerritoryRotationHelper.computeRotationInfo(responsibles, territory, events)
        val past = rotation.filter { it.hasWorkedPreviously }.sortedByDescending { it.lastAssignedDate }
        val suggested = rotation.filter { !it.hasWorkedPreviously && it.user.id != territory?.currentResponsibleId }
        val cycles = computeHistoryCycles(events)

        _uiState.update {
            it.copy(
                territory = territory,
                events = events,
                responsibles = responsibles,
                isLoading = isLoading,
                rotationInfo = rotation,
                pastWorkers = past,
                suggestedWorkers = suggested,
                historyCycles = cycles
            )
        }
    }

    private fun computeHistoryCycles(events: List<TerritoryEvent>): List<HistoryCycle> {
        val cycles = mutableListOf<HistoryCycle>()
        var currentResponsible = "Sistema / Não atribuído"
        var currentEvents = mutableListOf<TerritoryEvent>()

        for (event in events.sortedBy { it.timestamp }) {
            if (event.type == com.mraphaelpy.terriflow.domain.model.EventType.ASSIGNED) {
                if (currentEvents.isNotEmpty()) {
                    cycles.add(HistoryCycle(currentResponsible, currentEvents.reversed()))
                    currentEvents = mutableListOf()
                }
                currentResponsible = event.extra["responsibleName"] ?: event.userName
            } else if (event.type == com.mraphaelpy.terriflow.domain.model.EventType.TRANSFERRED) {
                if (currentEvents.isNotEmpty()) {
                    cycles.add(HistoryCycle(currentResponsible, currentEvents.reversed()))
                    currentEvents = mutableListOf()
                }
                currentResponsible = event.extra["toUserName"] ?: event.userName
            }
            currentEvents.add(event)
        }
        if (currentEvents.isNotEmpty()) {
            cycles.add(HistoryCycle(currentResponsible, currentEvents.reversed()))
        }
        return cycles.reversed()
    }

    fun assign(responsibleId: String) = performAction {
        assignTerritory(territoryId, responsibleId)
        _uiState.update { it.copy(actionSuccess = "Território atribuído com sucesso") }
    }

    fun start() = performAction {
        startTerritory(territoryId)
        _uiState.update { it.copy(actionSuccess = "Trabalho iniciado") }
    }

    fun pause() = performAction {
        pauseTerritory(territoryId)
        _uiState.update { it.copy(actionSuccess = "Trabalho pausado") }
    }

    fun complete(nextResponsibleId: String? = null) = performAction {
        completeTerritory(territoryId, nextResponsibleId)
        _uiState.update { it.copy(actionSuccess = "Território finalizado") }
    }

    fun returnTerritory() = performAction {
        returnTerritory(territoryId)
        _uiState.update { it.copy(actionSuccess = "Território devolvido") }
    }

    fun deleteTerritory() = performAction {
        territoryRepository.delete(territoryId)
        _uiState.update { it.copy(actionSuccess = "deleted") }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, actionSuccess = null) }
    }

    private fun performAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { block() }
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
