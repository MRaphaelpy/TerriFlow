package com.mraphaelpy.terriflow.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

data class DayGroup<T>(val label: String, val items: List<T>)

object DayGrouping {

    /** Agrupa itens já ordenados por data, com rótulos "Hoje", "Ontem" ou a data por extenso. */
    fun <T> group(items: List<T>, now: Date = Date(), dateOf: (T) -> Date): List<DayGroup<T>> {
        val today = dayKey(now)
        val yesterday = dayKey(Calendar.getInstance().apply { time = now; add(Calendar.DAY_OF_YEAR, -1) }.time)
        val currentYear = Calendar.getInstance().apply { time = now }.get(Calendar.YEAR)
        val dayFormat = SimpleDateFormat("EEEE, d 'de' MMMM", DateFormats.PT_BR)
        val dayWithYearFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", DateFormats.PT_BR)

        return items.groupBy { dayKey(dateOf(it)) }.map { (key, dayItems) ->
            val date = dateOf(dayItems.first())
            val label = when (key) {
                today -> "Hoje"
                yesterday -> "Ontem"
                else -> {
                    val year = Calendar.getInstance().apply { time = date }.get(Calendar.YEAR)
                    (if (year == currentYear) dayFormat else dayWithYearFormat).format(date)
                        .replaceFirstChar { it.uppercase(DateFormats.PT_BR) }
                }
            }
            DayGroup(label, dayItems)
        }
    }

    private fun dayKey(date: Date): Int = Calendar.getInstance().apply { time = date }
        .let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
}
