package com.mraphaelpy.terriflow.domain.model

fun TerritoryEvent.withCurrentNames(usersById: Map<String, User>): TerritoryEvent {
    fun currentName(id: String?): String? = id?.takeIf { it.isNotBlank() }?.let { usersById[it]?.name }

    val updatedExtra = extra.toMutableMap().apply {
        currentName(extra["responsibleId"])?.let { put("responsibleName", it) }
        currentName(extra["fromUserId"])?.let { put("fromUserName", it) }
        currentName(extra["toUserId"])?.let { put("toUserName", it) }
    }
    return copy(userName = currentName(userId) ?: userName, extra = updatedExtra)
}

fun List<TerritoryEvent>.withCurrentNames(users: List<User>): List<TerritoryEvent> {
    if (users.isEmpty()) return this
    val usersById = users.associateBy { it.id }
    return map { it.withCurrentNames(usersById) }
}
