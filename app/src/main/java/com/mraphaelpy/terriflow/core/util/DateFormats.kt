package com.mraphaelpy.terriflow.core.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateFormats {
    val PT_BR: Locale = Locale("pt", "BR")

    fun shortDateTime() = SimpleDateFormat("dd/MM HH:mm", PT_BR)
    fun shortDate()     = SimpleDateFormat("dd/MM/yy", PT_BR)
    fun fullDate()      = SimpleDateFormat("dd/MM/yyyy", PT_BR)
    fun fullDateTime()  = SimpleDateFormat("dd/MM/yy HH:mm", PT_BR)
}
