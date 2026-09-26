package com.mraphaelpy.terriflow.domain.model

import java.util.Date

data class Congregation(
    val id: String = "",
    val name: String = "",
    val code: String = "", // short invite code e.g. "TERRA-CE"
    val createdAt: Date = Date()
)
