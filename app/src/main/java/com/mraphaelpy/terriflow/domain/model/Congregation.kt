package com.mraphaelpy.terriflow.domain.model

import java.util.Date

data class Congregation(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val createdAt: Date = Date()
)
