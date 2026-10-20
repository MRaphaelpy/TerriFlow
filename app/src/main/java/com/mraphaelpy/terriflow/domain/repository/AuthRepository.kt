package com.mraphaelpy.terriflow.domain.repository

import com.mraphaelpy.terriflow.domain.model.User
import com.mraphaelpy.terriflow.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Autenticação e perfil do usuário logado.
 * Falhas de login/cadastro são lançadas como [com.mraphaelpy.terriflow.domain.model.AuthException].
 */
interface AuthRepository {
    val currentUserId: String?
    val isLoggedIn: Boolean

    /** Emite o uid a cada login/logout. */
    fun observeCurrentUserId(): Flow<String?>

    /** Perfil do usuário logado; acompanha trocas de conta. */
    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?

    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun logout()
    suspend fun sendPasswordReset(email: String)

    /** Registra o token FCM deste aparelho para receber notificações push. */
    suspend fun registerDeviceToken()
    suspend fun updateFcmToken(token: String)

    suspend fun updatePhotoUrl(photoUrl: String)
    suspend fun updateName(name: String)

    /** Descobre (e guarda localmente) a congregação do usuário logado. */
    suspend fun resolveAndSaveCongregationId(): String?

    /**
     * Garante que o usuário logado tenha perfil dentro da congregação.
     * Se o perfil já existir, papel e status atuais são preservados;
     * caso contrário é criado com [roleIfNew].
     */
    suspend fun ensureCongregationProfile(congregationId: String, roleIfNew: UserRole): User
}
