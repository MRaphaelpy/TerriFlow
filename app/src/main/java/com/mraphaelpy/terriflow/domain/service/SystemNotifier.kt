package com.mraphaelpy.terriflow.domain.service

interface SystemNotifier {
    fun show(title: String, body: String, territoryId: String? = null)
}
