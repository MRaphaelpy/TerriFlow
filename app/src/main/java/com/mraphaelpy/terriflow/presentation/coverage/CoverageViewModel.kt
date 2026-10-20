package com.mraphaelpy.terriflow.presentation.coverage

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mraphaelpy.terriflow.core.report.ReportExporter
import com.mraphaelpy.terriflow.core.report.ReportFormat
import com.mraphaelpy.terriflow.domain.model.AssignmentHistory
import com.mraphaelpy.terriflow.domain.model.CampaignCoverage
import com.mraphaelpy.terriflow.domain.model.ResponsibleStats
import com.mraphaelpy.terriflow.domain.model.ServiceYear
import com.mraphaelpy.terriflow.domain.model.ServiceYearCoverage
import com.mraphaelpy.terriflow.domain.model.TerritoryStatistics
import com.mraphaelpy.terriflow.domain.repository.CampaignRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.usecase.campaign.CreateCampaignUseCase
import com.mraphaelpy.terriflow.domain.usecase.campaign.DeleteCampaignUseCase
import com.mraphaelpy.terriflow.presentation.components.territoryPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

enum class CoverageTab(val label: String) {
    TERRITORIES("Territórios"),
    RESPONSIBLES("Dirigentes"),
    CAMPAIGNS("Campanhas")
}

data class CoverageUiState(
    val year: ServiceYear = ServiceYear.current(),
    val coverage: ServiceYearCoverage? = null,
    val staleCount: Int = 0,
    val coverages: List<TerritoryCoverage> = emptyList(),
    val sortOption: CoverageSortOption = CoverageSortOption.OLDEST_COMPLETED_FIRST,
    val responsibles: List<ResponsibleStats> = emptyList(),
    val tab: CoverageTab = CoverageTab.TERRITORIES,
    val campaigns: List<CampaignCoverage> = emptyList(),
    val palette: Map<String, Color> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CoverageViewModel @Inject constructor(
    territoryRepository: TerritoryRepository,
    private val eventRepository: TerritoryEventRepository,
    private val congregationRepository: CongregationRepository,
    private val reportExporter: ReportExporter,
    campaignRepository: CampaignRepository,
    private val createCampaign: CreateCampaignUseCase,
    private val deleteCampaign: DeleteCampaignUseCase
) : ViewModel() {

    private val year = MutableStateFlow(ServiceYear.current())
    private val sortOption = MutableStateFlow(CoverageSortOption.OLDEST_COMPLETED_FIRST)
    private val tab = MutableStateFlow(CoverageTab.TERRITORIES)

    private val _isSyncing = MutableStateFlow(true)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _shareIntent = MutableStateFlow<Intent?>(null)
    val shareIntent: StateFlow<Intent?> = _shareIntent

    private val data = combine(territoryRepository.observeAll(), eventRepository.observeAll()) { territories, events ->
        territories to AssignmentHistory.build(events)
    }

    val uiState: StateFlow<CoverageUiState> = combine(
        data, year, sortOption, tab, campaignRepository.observeAll()
    ) { (territories, periods), selectedYear, sort, selectedTab, campaigns ->
        CoverageUiState(
            campaigns = campaigns.map { TerritoryStatistics.campaignCoverage(territories, periods, it) },
            year = selectedYear,
            coverage = TerritoryStatistics.coverage(territories, periods, selectedYear),
            staleCount = TerritoryStatistics.notCompletedSince(territories, periods).size,
            coverages = CoverageCalculator.sort(CoverageCalculator.compute(territories, periods, selectedYear), sort),
            sortOption = sort,
            responsibles = TerritoryStatistics.byResponsible(periods, territories),
            tab = selectedTab,
            palette = territoryPalette(territories),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoverageUiState())

    val availableYears: List<ServiceYear> = ServiceYear.current().let { listOf(it, it.previous(), it.previous().previous()) }

    init {
        viewModelScope.launch {
            runCatching { eventRepository.syncAllFromRemote() }
            _isSyncing.value = false
        }
    }

    fun setYear(value: ServiceYear) {
        year.value = value
    }

    fun setSortOption(option: CoverageSortOption) {
        sortOption.value = option
        tab.value = CoverageTab.TERRITORIES
    }

    fun setTab(value: CoverageTab) {
        tab.value = value
    }

    fun exportS13(format: ReportFormat) {
        viewModelScope.launch {
            val (territories, periods) = data.first()
            val selectedYear = year.value
            val congregationName = runCatching {
                congregationRepository.getCurrentCongregationId()?.let { congregationRepository.getById(it)?.name }
            }.getOrNull()
            val table = S13ReportBuilder.build(territories, periods, selectedYear, congregationName)
            runCatching { reportExporter.export(table, format, "S-13 ${selectedYear.label.replace('/', '-')}") }
                .onSuccess { _shareIntent.value = reportExporter.shareIntent(it, format) }
        }
    }

    fun createCampaign(name: String, start: Date, end: Date) = runWithMessage("Campanha criada") {
        createCampaign.invoke(name, start, end)
    }

    fun deleteCampaign(campaignId: String) = runWithMessage("Campanha removida") {
        deleteCampaign.invoke(campaignId)
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun runWithMessage(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _message.value = runCatching { block() }.fold({ success }, { it.message ?: "Não foi possível concluir" })
        }
    }

    fun onShareHandled() {
        _shareIntent.value = null
    }
}
