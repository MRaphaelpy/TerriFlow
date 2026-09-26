package com.mraphaelpy.terriflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mraphaelpy.terriflow.data.remote.source.FirestoreCongregationSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CongregationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val congregationSource: FirestoreCongregationSource,
    private val userSource: FirestoreUserSource
) : CongregationRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("terriflow_congregation", Context.MODE_PRIVATE)

    override suspend fun getCurrentCongregationId(): String? {
        val cached = prefs.getString("congregation_id", null)
        if (!cached.isNullOrEmpty()) return cached

        // Fallback: se o cache local estiver vazio (ex: dados limpos ou login prévio),
        // busca no documento global do usuário no Firestore
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val remoteId = runCatching { userSource.getCongregationIdForUser(uid) }.getOrNull()
        if (!remoteId.isNullOrEmpty()) {
            saveCongregationIdLocally(remoteId)
            return remoteId
        }
        return null
    }

    override suspend fun saveCongregationIdLocally(id: String) {
        prefs.edit().putString("congregation_id", id).apply()
    }

    override suspend fun clearCongregationId() {
        prefs.edit().remove("congregation_id").apply()
    }

    override suspend fun create(name: String, adminUserId: String): Congregation {
        // Bloqueia criação duplicada — verifica localmente e no Firestore
        val existingLocal = getCurrentCongregationId()
        if (!existingLocal.isNullOrEmpty()) {
            val existing = congregationSource.getById(existingLocal)
            if (existing != null) {
                throw IllegalStateException(
                    "Você já pertence à congregação \"${existing.name}\". " +
                    "Não é possível criar outra."
                )
            }
        }
        val existingRemote = userSource.getCongregationIdForUser(adminUserId)
        if (!existingRemote.isNullOrEmpty()) {
            val existing = congregationSource.getById(existingRemote)
            if (existing != null) {
                // Sincroniza localmente e rejeita
                saveCongregationIdLocally(existingRemote)
                throw IllegalStateException(
                    "Você já pertence à congregação \"${existing.name}\". " +
                    "Não é possível criar outra."
                )
            }
        }

        val code = generateCode(name)
        val congregation = Congregation(
            id = UUID.randomUUID().toString(),
            name = name,
            code = code
        )
        congregationSource.create(congregation)
        userSource.saveGlobalPointer(adminUserId, congregation.id)
        saveCongregationIdLocally(congregation.id)
        return congregation
    }

    override suspend fun joinByCode(code: String, userId: String): Congregation {
        val congregation = congregationSource.findByCode(code)
            ?: throw IllegalArgumentException("Código de congregação inválido: $code")
        userSource.saveGlobalPointer(userId, congregation.id)
        saveCongregationIdLocally(congregation.id)
        return congregation
    }

    override suspend fun getById(id: String): Congregation? =
        congregationSource.getById(id)

    private fun generateCode(name: String): String {
        val base = name.uppercase()
            .replace(Regex("[^A-Z0-9 ]"), "")
            .trim()
            .replace(" ", "-")
            .take(10)
        val suffix = (1000..9999).random()
        return "$base-$suffix"
    }
}
