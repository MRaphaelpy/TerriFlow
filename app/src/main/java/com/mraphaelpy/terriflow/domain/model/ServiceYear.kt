package com.mraphaelpy.terriflow.domain.model

import java.util.Calendar
import java.util.Date

data class ServiceYear(val startYear: Int) {

    val start: Date get() = firstOfSeptember(startYear)
    val endExclusive: Date get() = firstOfSeptember(startYear + 1)
    val label: String get() = "$startYear/${startYear + 1}"

    operator fun contains(date: Date): Boolean = !date.before(start) && date.before(endExclusive)

    fun previous(): ServiceYear = ServiceYear(startYear - 1)

    companion object {
        fun of(date: Date): ServiceYear {
            val calendar = Calendar.getInstance().apply { time = date }
            val year = calendar.get(Calendar.YEAR)
            return ServiceYear(if (calendar.get(Calendar.MONTH) >= Calendar.SEPTEMBER) year else year - 1)
        }

        fun current(): ServiceYear = of(Date())

        private fun firstOfSeptember(year: Int): Date = Calendar.getInstance().apply {
            clear()
            set(year, Calendar.SEPTEMBER, 1, 0, 0, 0)
        }.time
    }
}
