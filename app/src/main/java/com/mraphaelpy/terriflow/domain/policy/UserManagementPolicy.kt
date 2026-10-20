package com.mraphaelpy.terriflow.domain.policy

import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole

object UserManagementPolicy {

    fun canRename(actor: User?, target: User): Boolean =
        actor != null && actor.active && (actor.id == target.id || actor.isAdmin)

    fun roleChanges(actor: User?, target: User): List<UserRole> {
        if (actor == null || !actor.active || !actor.isAdmin) return emptyList()
        if (target.isSuperAdmin) return emptyList()
        if (actor.id == target.id && !actor.isSuperAdmin) return emptyList()
        return when (target.role) {
            UserRole.ADMIN -> listOf(UserRole.RESPONSIBLE)
            UserRole.RESPONSIBLE -> listOf(UserRole.ADMIN)
            UserRole.SUPER_ADMIN -> emptyList()
        }
    }

    fun canToggleActive(actor: User?, target: User): Boolean =
        actor != null && actor.active && actor.isAdmin && actor.id != target.id && !target.isSuperAdmin
}
