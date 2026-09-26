package com.mraphaelpy.terriflow.core.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateFormats {
    val PT_BR: Locale = Locale("pt", "BR")

    fun shortDateTime() = SimpleDateFormat("dd/MM HH:mm", PT_BR)     // ex: 01/01 14:30
    fun shortDate()     = SimpleDateFormat("dd/MM/yy", PT_BR)         // ex: 01/01/24
    fun fullDate()      = SimpleDateFormat("dd/MM/yyyy", PT_BR)       // ex: 01/01/2024
    fun fullDateTime()  = SimpleDateFormat("dd/MM/yy HH:mm", PT_BR)  // ex: 01/01/24 14:30
}
