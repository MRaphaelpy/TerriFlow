package com.mraphaelpy.terriflow.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole

data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "RESPONSIBLE",
    val createdAt: Timestamp = Timestamp.now(),
    val active: Boolean = true,
    val photoUrl: String? = null,
    val fcmTokens: List<String> = emptyList(),
    val congregationId: String = ""
) {
    fun toDomain() = User(
        id = id,
        name = name,
        email = email,
        role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.RESPONSIBLE),
        createdAt = createdAt.toDate(),
        active = active,
        photoUrl = photoUrl,
        fcmTokens = fcmTokens,
        congregationId = congregationId
    )

    fun toMap() = mapOf(
        "name" to name,
        "email" to email,
        "role" to role,
        "createdAt" to createdAt,
        "active" to active,
        "photoUrl" to photoUrl,
        "fcmTokens" to fcmTokens,
        "congregationId" to congregationId
    )

    companion object {
        fun fromDocument(doc: DocumentSnapshot): UserDto? {
            if (!doc.exists()) return null
            return UserDto(
                id = doc.id,
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                role = doc.getString("role") ?: "RESPONSIBLE",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                active = doc.getBoolean("active") ?: true,
                photoUrl = doc.getString("photoUrl"),
                fcmTokens = (doc.get("fcmTokens") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                congregationId = doc.getString("congregationId") ?: ""
            )
        }

        fun fromDomain(user: User): UserDto {
            return UserDto(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role.name,
                createdAt = Timestamp(user.createdAt),
                active = user.active,
                photoUrl = user.photoUrl,
                fcmTokens = user.fcmTokens,
                congregationId = user.congregationId
            )
        }
    }
}
