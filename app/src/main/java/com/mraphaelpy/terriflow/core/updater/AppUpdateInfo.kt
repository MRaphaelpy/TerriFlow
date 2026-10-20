package com.mraphaelpy.terriflow.core.updater

import java.io.File

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val apkSize: Long = 0L,
    val sha256: String? = null
)

sealed interface DownloadResult {
    data class Success(val file: File) : DownloadResult
    data class Failure(val message: String) : DownloadResult
}
