package com.mraphaelpy.terriflow.domain.model

import java.util.Date

enum class UserRole {
    SUPER_ADMIN, ADMIN, RESPONSIBLE;

    val isAdmin: Boolean get() = this == SUPER_ADMIN || this == ADMIN
}

fun UserRole.label(): String = when (this) {
    UserRole.SUPER_ADMIN -> "Super Administrador"
    UserRole.ADMIN -> "Administrador"
    UserRole.RESPONSIBLE -> "Responsável"
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.RESPONSIBLE,
    val createdAt: Date = Date(),
    val active: Boolean = true,
    val photoUrl: String? = null,
    val fcmTokens: List<String> = emptyList(),
    val congregationId: String = ""
) {
    val isAdmin: Boolean get() = role.isAdmin
    val isSuperAdmin: Boolean get() = role == UserRole.SUPER_ADMIN
}
