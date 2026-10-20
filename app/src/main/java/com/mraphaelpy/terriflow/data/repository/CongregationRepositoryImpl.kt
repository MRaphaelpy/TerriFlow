package com.mraphaelpy.terriflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.mraphaelpy.terriflow.data.remote.source.FirestoreCongregationSource
import com.mraphaelpy.terriflow.data.remote.source.FirestoreUserSource
import com.mraphaelpy.terriflow.domain.model.Congregation
import com.mraphaelpy.terriflow.domain.model.DomainException
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CongregationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: FirebaseAuth,
    private val congregationSource: FirestoreCongregationSource,
    private val userSource: FirestoreUserSource
) : CongregationRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getCurrentCongregationId(): String? {
        prefs.getString(KEY_CONGREGATION_ID, null)?.takeIf { it.isNotEmpty() }?.let { return it }

        // Cache local vazio (dados limpos ou primeiro login neste aparelho):
        // busca no documento global do usuário no Firestore
        val uid = auth.currentUser?.uid ?: return null
        val remoteId = runCatching { userSource.getCongregationIdForUser(uid) }.getOrNull()
        if (remoteId.isNullOrEmpty()) return null
        saveCongregationIdLocally(remoteId)
        return remoteId
    }

    override suspend fun saveCongregationIdLocally(id: String) {
        prefs.edit { putString(KEY_CONGREGATION_ID, id) }
    }

    override suspend fun clearCongregationId() {
        prefs.edit { remove(KEY_CONGREGATION_ID) }
    }

    override suspend fun create(name: String, adminUserId: String): Congregation {
        getCurrentCongregationId()
            ?.let { congregationSource.getById(it) }
            ?.let { throw DomainException.AlreadyInCongregation(it.name) }

        val congregation = Congregation(
            id = UUID.randomUUID().toString(),
            name = name,
            code = generateCode(name)
        )
        congregationSource.create(congregation, createdBy = adminUserId)
        userSource.saveGlobalPointer(adminUserId, congregation.id)
        saveCongregationIdLocally(congregation.id)
        return congregation
    }

    override suspend fun joinByCode(code: String, userId: String): Congregation {
        val congregation = congregationSource.findByCode(code)
            ?: throw DomainException.InvalidCongregationCode(code)
        userSource.saveGlobalPointer(userId, congregation.id)
        saveCongregationIdLocally(congregation.id)
        return congregation
    }

    override suspend fun getById(id: String): Congregation? =
        congregationSource.getById(id)

    private fun generateCode(name: String): String {
        // Remove acentos antes de filtrar, para "São José" virar "SAO-JOSE" e não "SO-JOS"
        val base = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .uppercase()
            .replace(Regex("[^A-Z0-9 ]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
            .take(10)
            .trimEnd('-')
        val suffix = (1000..9999).random()
        return if (base.isEmpty()) "CONG-$suffix" else "$base-$suffix"
    }

    private companion object {
        const val PREFS_NAME = "terriflow_congregation"
        const val KEY_CONGREGATION_ID = "congregation_id"
    }
}
