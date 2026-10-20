package com.mraphaelpy.terriflow.domain.service

/** Exibe notificações do sistema operacional (bandeja do Android). */
interface SystemNotifier {
    fun show(title: String, body: String, territoryId: String? = null)
}
