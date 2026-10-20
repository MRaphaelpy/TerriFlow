package com.mraphaelpy.terriflow.domain.model

import java.util.Date
import java.util.UUID

/** Período especial de pregação (ex.: Celebração) em que se acompanha a cobertura. */
data class Campaign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val start: Date,
    val end: Date,
    val createdBy: String = ""
) {
    operator fun contains(date: Date): Boolean = !date.before(start) && !date.after(end)
    fun isActive(now: Date = Date()): Boolean = now in this
}

data class CampaignCoverage(
    val campaign: Campaign,
    val completed: List<Territory>,
    val inProgress: List<Territory>,
    val notWorked: List<Territory>
) {
    val total: Int get() = completed.size + inProgress.size + notWorked.size
    val percent: Int get() = if (total == 0) 0 else completed.size * 100 / total
}
