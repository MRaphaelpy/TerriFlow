package com.mraphaelpy.terriflow.ui.theme

import androidx.compose.ui.graphics.Color
import com.mraphaelpy.terriflow.domain.repository.AppColor

val AppColor.seed: Color
    get() = when (this) {
        AppColor.DEFAULT, AppColor.DYNAMIC -> Color(0xFF006C4C)
        AppColor.TEAL -> Color(0xFF00696E)
        AppColor.BLUE -> Color(0xFF006495)
        AppColor.INDIGO -> Color(0xFF4355B9)
        AppColor.PURPLE -> Color(0xFF6D4DAA)
        AppColor.PINK -> Color(0xFFA23D74)
        AppColor.RED -> Color(0xFFB3261E)
        AppColor.ORANGE -> Color(0xFF904D00)
        AppColor.AMBER -> Color(0xFF7D5700)
        AppColor.OLIVE -> Color(0xFF5B6300)
        AppColor.BROWN -> Color(0xFF77574A)
    }

val AppColor.label: String
    get() = when (this) {
        AppColor.DEFAULT -> "Verde"
        AppColor.TEAL -> "Turquesa"
        AppColor.BLUE -> "Azul"
        AppColor.INDIGO -> "Índigo"
        AppColor.PURPLE -> "Roxo"
        AppColor.PINK -> "Rosa"
        AppColor.RED -> "Vermelho"
        AppColor.ORANGE -> "Laranja"
        AppColor.AMBER -> "Âmbar"
        AppColor.OLIVE -> "Oliva"
        AppColor.BROWN -> "Marrom"
        AppColor.DYNAMIC -> "Sistema"
    }
