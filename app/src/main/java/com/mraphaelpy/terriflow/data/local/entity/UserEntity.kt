package com.mraphaelpy.terriflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: Date,
    val active: Boolean,
    val photoUrl: String? = null,
    val fcmTokens: List<String> = emptyList(),
    val congregationId: String = "",
    val synced: Boolean = true,
    val updatedAt: Date = Date(),
    val deletedAt: Date? = null
) {
    fun toDomain() = User(
        id = id,
        name = name,
        email = email,
        role = UserRole.valueOf(role),
        createdAt = createdAt,
        active = active,
        photoUrl = photoUrl,
        fcmTokens = fcmTokens,
        congregationId = congregationId
    )

    companion object {
        fun fromDomain(user: User, synced: Boolean = true) = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role.name,
            createdAt = user.createdAt,
            active = user.active,
            photoUrl = user.photoUrl,
            fcmTokens = user.fcmTokens,
            congregationId = user.congregationId,
            synced = synced,
            updatedAt = Date()
        )
    }
}
