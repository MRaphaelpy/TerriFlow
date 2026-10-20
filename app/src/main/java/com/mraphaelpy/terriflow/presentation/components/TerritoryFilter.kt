package com.mraphaelpy.terriflow.presentation.components

import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import java.text.Normalizer

data class TerritoryFilter(
    val status: TerritoryStatus? = null,
    val onlyMine: Boolean = false,
    val query: String = ""
) {
    val isActive: Boolean get() = status != null || onlyMine || query.isNotBlank()

    fun apply(territories: List<Territory>, currentUserId: String?): List<Territory> {
        val normalizedQuery = query.normalizeForSearch()
        return territories.filter { territory ->
            (status == null || territory.status == status) &&
                (!onlyMine || (currentUserId != null && territory.currentResponsibleId == currentUserId)) &&
                (normalizedQuery.isEmpty() || territory.matches(normalizedQuery))
        }
    }

    private fun Territory.matches(normalizedQuery: String): Boolean =
        listOfNotNull(code, name, location, currentResponsibleName)
            .any { it.normalizeForSearch().contains(normalizedQuery) }

    private fun String.normalizeForSearch(): String =
        Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
}
