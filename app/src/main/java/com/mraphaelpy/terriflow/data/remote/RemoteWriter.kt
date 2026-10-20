package com.mraphaelpy.terriflow.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.mraphaelpy.terriflow.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emite gravações no Firestore sem bloquear quem chamou.
 *
 * O Firestore aplica a gravação no cache local na hora e a envia ao servidor
 * (na mesma ordem em que foram emitidas) assim que houver conexão. Aguardar a
 * confirmação do servidor na UI fazia as ações travarem indefinidamente offline.
 */
@Singleton
class RemoteWriter @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    fun write(tag: String, write: () -> Task<*>, onConfirmed: suspend () -> Unit = {}) {
        val task = runCatching(write).getOrElse {
            Log.e(tag, "Falha ao emitir gravação remota", it)
            return
        }
        scope.launch {
            runCatching { task.await() }
                .onSuccess { onConfirmed() }
                .onFailure { Log.e(tag, "Gravação remota recusada", it) }
        }
    }
}
