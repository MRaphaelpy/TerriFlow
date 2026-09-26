package com.mraphaelpy.terriflow.domain.model

import java.util.Date

enum class UserRole { SUPER_ADMIN, ADMIN, RESPONSIBLE }

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.RESPONSIBLE,
    val createdAt: Date = Date(),
    val active: Boolean = true,
    val photoUrl: String? = null,
    val fcmTokens: List<String> = emptyList()
)
