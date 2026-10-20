package com.mraphaelpy.terriflow.presentation.components

import com.mraphaelpy.terriflow.core.util.normalizedForSearch
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus

data class TerritoryFilter(
    val status: TerritoryStatus? = null,
    val onlyMine: Boolean = false,
    val query: String = ""
) {
    val isActive: Boolean get() = status != null || onlyMine || query.isNotBlank()

    fun apply(territories: List<Territory>, currentUserId: String?): List<Territory> {
        val normalizedQuery = query.normalizedForSearch()
        return territories.filter { territory ->
            (status == null || territory.status == status) &&
                (!onlyMine || (currentUserId != null && territory.currentResponsibleId == currentUserId)) &&
                (normalizedQuery.isEmpty() || territory.matches(normalizedQuery))
        }
    }

    private fun Territory.matches(normalizedQuery: String): Boolean =
        listOfNotNull(code, name, location, currentResponsibleName)
            .any { it.normalizedForSearch().contains(normalizedQuery) }

}
