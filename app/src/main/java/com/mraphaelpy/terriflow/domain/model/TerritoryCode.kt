package com.mraphaelpy.terriflow.domain.model

object TerritoryCode {
    const val PREFIX = "T-"
    private val PATTERN = Regex("""^T-(\d+)$""")

    fun format(number: Int): String = PREFIX + "%05d".format(number)

    fun numberOf(code: String): Int? = PATTERN.find(code.trim())?.groupValues?.get(1)?.toIntOrNull()

    fun nextAfter(codes: Iterable<String>): String =
        format((codes.mapNotNull(::numberOf).maxOrNull() ?: 0) + 1)
}
