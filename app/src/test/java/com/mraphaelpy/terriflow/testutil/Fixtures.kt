package com.mraphaelpy.terriflow.testutil

import com.mraphaelpy.terriflow.domain.model.Territory
import com.mraphaelpy.terriflow.domain.model.TerritoryStatus
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import java.util.Date

object Fixtures {
    val admin = User(id = "admin", name = "Admin", role = UserRole.ADMIN, congregationId = "cong")
    val publisher = User(id = "pub", name = "Publicador", role = UserRole.RESPONSIBLE, congregationId = "cong")
    val otherPublisher = User(id = "pub2", name = "Outro", role = UserRole.RESPONSIBLE, congregationId = "cong")

    fun territory(
        status: TerritoryStatus = TerritoryStatus.AVAILABLE,
        responsible: User? = null,
        assignedAt: Date? = null,
        startedAt: Date? = null
    ) = Territory(
        id = "t1",
        code = "T-00001",
        name = "Centro",
        status = status,
        currentResponsibleId = responsible?.id,
        currentResponsibleName = responsible?.name,
        assignedAt = assignedAt,
        startedAt = startedAt
    )

    fun daysAgo(days: Long, from: Date = Date()) = Date(from.time - days * 24 * 60 * 60 * 1000)
}
