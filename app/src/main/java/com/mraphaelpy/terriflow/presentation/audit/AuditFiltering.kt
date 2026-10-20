package com.mraphaelpy.terriflow.presentation.audit

import com.mraphaelpy.terriflow.domain.model.EventType
import com.mraphaelpy.terriflow.domain.model.ServiceYear
import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AuditPeriod(val label: String) {
    LAST_30_DAYS("Últimos 30 dias"),
    THIS_MONTH("Este mês"),
    SERVICE_YEAR("Ano de serviço atual"),
    PREVIOUS_SERVICE_YEAR("Ano de serviço anterior"),
    ALL("Todo o período");

    fun contains(date: Date, now: Date): Boolean = when (this) {
        LAST_30_DAYS -> !date.before(Calendar.getInstance().apply { time = now; add(Calendar.DAY_OF_YEAR, -30) }.time)
        THIS_MONTH -> Calendar.getInstance().let { calendar ->
            calendar.time = now
            val month = calendar.get(Calendar.MONTH) to calendar.get(Calendar.YEAR)
            calendar.time = date
            month == (calendar.get(Calendar.MONTH) to calendar.get(Calendar.YEAR))
        }
        SERVICE_YEAR -> date in ServiceYear.of(now)
        PREVIOUS_SERVICE_YEAR -> date in ServiceYear.of(now).previous()
        ALL -> true
    }
}

data class AuditFilter(
    val period: AuditPeriod = AuditPeriod.LAST_30_DAYS,
    val type: EventType? = null,
    val territoryId: String? = null,
    val userId: String? = null
) {
    val isActive: Boolean get() = type != null || territoryId != null || userId != null

    fun matches(event: TerritoryEvent, now: Date = Date()): Boolean =
        period.contains(event.timestamp, now) &&
            (type == null || event.type == type) &&
            (territoryId == null || event.territoryId == territoryId) &&
            (userId == null || userId in involvedUsers(event))

    private fun involvedUsers(event: TerritoryEvent): Set<String> = setOfNotNull(
        event.userId, event.extra["responsibleId"], event.extra["toUserId"], event.extra["fromUserId"]
    )
}

data class AuditEntry(val event: TerritoryEvent, val territory: Territory?)

data class AuditDay(val label: String, val entries: List<AuditEntry>)

object AuditGrouping {
    private val locale = Locale("pt", "BR")

    fun byDay(entries: List<AuditEntry>, now: Date = Date()): List<AuditDay> {
        val dayFormat = SimpleDateFormat("EEEE, d 'de' MMMM", locale)
        val dayWithYearFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", locale)
        val today = dayKey(now)
        val yesterday = dayKey(Calendar.getInstance().apply { time = now; add(Calendar.DAY_OF_YEAR, -1) }.time)
        val currentYear = Calendar.getInstance().apply { time = now }.get(Calendar.YEAR)

        return entries.groupBy { dayKey(it.event.timestamp) }.map { (key, dayEntries) ->
            val date = dayEntries.first().event.timestamp
            val label = when (key) {
                today -> "Hoje"
                yesterday -> "Ontem"
                else -> {
                    val year = Calendar.getInstance().apply { time = date }.get(Calendar.YEAR)
                    (if (year == currentYear) dayFormat else dayWithYearFormat).format(date)
                        .replaceFirstChar { it.uppercase(locale) }
                }
            }
            AuditDay(label, dayEntries)
        }
    }

    private fun dayKey(date: Date): Int = Calendar.getInstance().apply { time = date }
        .let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
}

/** Descreve as alterações registradas num evento de edição ("Nome: A → B"). */
fun describeChanges(event: TerritoryEvent): List<String> {
    if (event.type != EventType.UPDATED) return emptyList()
    val labels = listOf(
        "code" to "Código",
        "name" to "Nome",
        "location" to "Localização",
        "description" to "Descrição",
        "notes" to "Observações"
    )
    return buildList {
        labels.forEach { (key, label) ->
            val after = event.extra[key] ?: return@forEach
            val before = event.extra["${key}From"]
            add(if (before != null) "$label: ${before.ifBlank { "(vazio)" }} → ${after.ifBlank { "(vazio)" }}" else "$label: $after")
        }
        if (event.extra["pinChanged"] == "true") add("Localização no mapa alterada")
        if (event.extra["boundaryChanged"] == "true") add("Contorno alterado")
    }
}
