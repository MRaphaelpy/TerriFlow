package com.mraphaelpy.terriflow.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.mraphaelpy.terriflow.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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
