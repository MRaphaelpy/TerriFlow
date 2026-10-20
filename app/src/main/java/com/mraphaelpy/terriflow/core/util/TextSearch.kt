package com.mraphaelpy.terriflow.core.util

import java.text.Normalizer

private val diacritics = Regex("\\p{M}")

fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(diacritics, "").lowercase()
