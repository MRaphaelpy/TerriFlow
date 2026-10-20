package com.mraphaelpy.terriflow.presentation.components

import com.mraphaelpy.terriflow.domain.model.TerritoryStatus

val TerritoryStatusDisplayOrder = listOf(
    TerritoryStatus.IN_PROGRESS,
    TerritoryStatus.ASSIGNED,
    TerritoryStatus.PAUSED,
    TerritoryStatus.RETURNED,
    TerritoryStatus.AVAILABLE,
    TerritoryStatus.COMPLETED,
    TerritoryStatus.ARCHIVED
)
