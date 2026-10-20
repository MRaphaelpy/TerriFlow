package com.mraphaelpy.terriflow.presentation.audit

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.report.ReportColumn
import com.mraphaelpy.terriflow.core.report.ReportExporter
import com.mraphaelpy.terriflow.core.report.ReportFormat
import com.mraphaelpy.terriflow.core.report.ReportRow
import com.mraphaelpy.terriflow.core.report.ReportTable
import com.mraphaelpy.terriflow.core.util.DateFormats
import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.label
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
import com.mraphaelpy.terriflow.presentation.components.territoryPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AuditUiState(
    val days: List<AuditDay> = emptyList(),
    val matchingCount: Int = 0,
    val hasMore: Boolean = false,
    val filter: AuditFilter = AuditFilter(),
    val territories: List<Territory> = emptyList(),
    val users: List<User> = emptyList(),
    val palette: Map<String, Color> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val eventRepository: TerritoryEventRepository,
    territoryRepository: TerritoryRepository,
    userRepository: UserRepository,
    private val reportExporter: ReportExporter
) : ViewModel() {

    private val filter = MutableStateFlow(AuditFilter())
    private val pageSize = MutableStateFlow(PAGE_SIZE)

    private val _isSyncing = MutableStateFlow(true)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _shareIntent = MutableStateFlow<Intent?>(null)
    val shareIntent: StateFlow<Intent?> = _shareIntent

    private val matching = combine(
        eventRepository.observeAll(),
        territoryRepository.observeAll(),
        filter
    ) { events, territories, currentFilter ->
        val now = Date()
        val byId = territories.associateBy { it.id }
        events.filter { currentFilter.matches(it, now) }
            .sortedByDescending { it.timestamp }
            .map { AuditEntry(it, byId[it.territoryId]) } to territories
    }

    val uiState: StateFlow<AuditUiState> = combine(
        matching,
        userRepository.observeAll(),
        filter,
        pageSize
    ) { (entries, territories), users, currentFilter, size ->
        AuditUiState(
            days = AuditGrouping.byDay(entries.take(size)),
            matchingCount = entries.size,
            hasMore = entries.size > size,
            filter = currentFilter,
            territories = territories.sortedBy { it.code },
            users = users,
            palette = territoryPalette(territories),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuditUiState())

    init {
        viewModelScope.launch {
            runCatching { eventRepository.syncAllFromRemote() }
            _isSyncing.value = false
        }
    }

    fun setPeriod(period: AuditPeriod) = updateFilter { it.copy(period = period) }
    fun setType(type: EventType?) = updateFilter { it.copy(type = type) }
    fun setTerritory(territoryId: String?) = updateFilter { it.copy(territoryId = territoryId) }
    fun setUser(userId: String?) = updateFilter { it.copy(userId = userId) }
    fun clearFilters() = updateFilter { AuditFilter(period = it.period) }

    fun loadMore() = pageSize.update { it + PAGE_SIZE }

    fun export(format: ReportFormat) {
        viewModelScope.launch {
            val state = uiState.value
            val entries = matching.first().first
            val formatter = DateFormats.fullDateTime()
            val table = ReportTable(
                title = "Auditoria de territórios",
                subtitle = "${state.filter.period.label} · ${entries.size} registros · gerado em ${formatter.format(Date())}",
                columns = listOf(
                    ReportColumn("Data", 1.1f),
                    ReportColumn("Território", 1.6f),
                    ReportColumn("Ação", 1.1f),
                    ReportColumn("Por", 1.3f),
                    ReportColumn("Detalhes", 2.2f)
                ),
                rows = entries.map { entry ->
                    val event = entry.event
                    ReportRow.Cells(
                        listOf(
                            formatter.format(event.timestamp),
                            entry.territory?.let { "${it.code} — ${it.name}" } ?: "Removido",
                            event.type.label(),
                            event.userName,
                            details(entry)
                        )
                    )
                }
            )
            runCatching { reportExporter.export(table, format, "auditoria-${state.filter.period.name.lowercase()}") }
                .onSuccess { _shareIntent.value = reportExporter.shareIntent(it, format) }
        }
    }

    fun onShareHandled() {
        _shareIntent.value = null
    }

    private fun updateFilter(transform: (AuditFilter) -> AuditFilter) {
        filter.update(transform)
        pageSize.value = PAGE_SIZE
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}

internal fun details(entry: AuditEntry): String {
    val extra = entry.event.extra
    return buildList {
        extra["responsibleName"]?.let { add("Para: $it") }
        extra["toUserName"]?.let { add("Para: $it") }
        addAll(describeChanges(entry.event))
    }.joinToString(" · ")
}
