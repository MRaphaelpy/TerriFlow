package com.mraphaelpy.terriflow.domain.model

/** Formato dos códigos gerados automaticamente: `T-00001`, `T-00002`... */
object TerritoryCode {
    const val PREFIX = "T-"
    private val PATTERN = Regex("""^T-(\d+)$""")

    fun format(number: Int): String = PREFIX + "%05d".format(number)

    /** Número sequencial do código, ou `null` se for um código personalizado. */
    fun numberOf(code: String): Int? = PATTERN.find(code.trim())?.groupValues?.get(1)?.toIntOrNull()

    /** Próximo código livre considerando apenas os códigos no formato automático. */
    fun nextAfter(codes: Iterable<String>): String =
        format((codes.mapNotNull(::numberOf).maxOrNull() ?: 0) + 1)
}
