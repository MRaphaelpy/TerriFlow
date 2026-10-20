package com.mraphaelpy.terriflow.domain.policy

import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryAction
import com.mraphaelpy.terriflow.domain.model.TerritoryNote
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User

object TerritoryActionPolicy {

    private val assignableStatuses = setOf(
        TerritoryStatus.AVAILABLE, TerritoryStatus.RETURNED, TerritoryStatus.COMPLETED
    )
    private val startableStatuses = setOf(TerritoryStatus.ASSIGNED, TerritoryStatus.PAUSED)
    private val completableStatuses = setOf(TerritoryStatus.IN_PROGRESS, TerritoryStatus.PAUSED)
    private val remindableStatuses = setOf(TerritoryStatus.ASSIGNED, TerritoryStatus.IN_PROGRESS, TerritoryStatus.PAUSED)
    private val returnableStatuses = setOf(
        TerritoryStatus.ASSIGNED, TerritoryStatus.IN_PROGRESS, TerritoryStatus.PAUSED, TerritoryStatus.COMPLETED
    )

    fun canPerform(user: User?, territory: Territory, action: TerritoryAction): Boolean {
        if (user == null || !user.active) return false
        val isAdmin = user.isAdmin
        val canOperate = isAdmin || territory.currentResponsibleId == user.id
        val status = territory.status

        return when (action) {
            TerritoryAction.ASSIGN -> isAdmin && status in assignableStatuses
            TerritoryAction.START -> canOperate && status in startableStatuses
            TerritoryAction.PAUSE -> canOperate && status == TerritoryStatus.IN_PROGRESS
            TerritoryAction.COMPLETE -> canOperate && status in completableStatuses
            TerritoryAction.RETURN -> canOperate && status in returnableStatuses
            TerritoryAction.REMIND -> isAdmin && status in remindableStatuses &&
                territory.currentResponsibleId != null && territory.currentResponsibleId != user.id
            TerritoryAction.CREATE, TerritoryAction.EDIT, TerritoryAction.DELETE -> isAdmin
        }
    }

    /** Anotações (inclusive "não visitar") são restritas a admins e ao dirigente atual. */
    fun canAccessNotes(user: User?, territory: Territory): Boolean =
        user != null && user.active && (user.isAdmin || territory.currentResponsibleId == user.id)

    fun canDeleteNote(user: User?, note: TerritoryNote): Boolean =
        user != null && user.active && (user.isAdmin || note.authorId == user.id)

    fun canCreate(user: User?): Boolean = user != null && user.active && user.isAdmin

    fun allowedActions(user: User?, territory: Territory): Set<TerritoryAction> =
        (TerritoryAction.entries - TerritoryAction.CREATE).filterTo(mutableSetOf()) { canPerform(user, territory, it) }
}
