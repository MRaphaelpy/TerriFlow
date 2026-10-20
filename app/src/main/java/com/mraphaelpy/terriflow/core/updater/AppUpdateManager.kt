package com.mraphaelpy.terriflow.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.mraphaelpy.terriflow.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("terriflow_updater", Context.MODE_PRIVATE)

    private val apkFile: File get() = File(File(context.getExternalFilesDir(null), "Download"), APK_NAME)

    val currentVersion: String
        get() = runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: BuildConfig.VERSION_NAME

    fun isSnoozed(version: String): Boolean {
        if (prefs.getString(KEY_SNOOZED_VERSION, null) != VersionComparator.clean(version)) return false
        val until = prefs.getLong(KEY_SNOOZED_UNTIL, 0L)
        return System.currentTimeMillis() < until
    }

    fun snooze(version: String) {
        prefs.edit {
            putString(KEY_SNOOZED_VERSION, VersionComparator.clean(version))
            putLong(KEY_SNOOZED_UNTIL, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(SNOOZE_HOURS))
        }
    }

    fun cleanUpInstalledUpdate() {
        val downloadedVersion = prefs.getString(KEY_DOWNLOADED_VERSION, null) ?: return
        if (!VersionComparator.isNewer(downloadedVersion, currentVersion)) {
            apkFile.delete()
            prefs.edit { remove(KEY_DOWNLOADED_VERSION) }
        }
    }

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open(RELEASES_API_URL).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })

            val tagName = json.optString("tag_name", "")
            val assets = json.optJSONArray("assets") ?: return@runCatching null
            val apk = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: return@runCatching null

            val current = currentVersion
            AppUpdateInfo(
                hasUpdate = VersionComparator.isNewer(tagName, current),
                currentVersion = current,
                latestVersion = VersionComparator.clean(tagName),
                releaseTitle = json.optString("name", tagName),
                changelog = ReleaseNotes.clean(json.optString("body", "")),
                downloadUrl = apk.optString("browser_download_url"),
                apkSize = apk.optLong("size", 0L),
                sha256 = apk.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")
            )
        }.getOrNull()
    }

    suspend fun downloadApk(update: AppUpdateInfo, onProgress: (Float) -> Unit): DownloadResult = withContext(Dispatchers.IO) {
        val file = apkFile.apply { parentFile?.mkdirs(); delete() }
        try {
            val connection = followRedirects(open(update.downloadUrl))
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext DownloadResult.Failure("Servidor respondeu ${connection.responseCode}. Tente novamente.")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.apkSize
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var lastPercent = -1

            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        val percent = if (total > 0) (downloaded * 100 / total).toInt() else 0
                        if (percent != lastPercent) {
                            lastPercent = percent
                            withContext(Dispatchers.Main) { onProgress((percent / 100f).coerceIn(0f, 1f)) }
                        }
                    }
                }
            }

            if (update.apkSize > 0 && downloaded != update.apkSize) {
                file.delete()
                return@withContext DownloadResult.Failure("Download incompleto. Verifique sua conexão e tente novamente.")
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (update.sha256 != null && !hash.equals(update.sha256, ignoreCase = true)) {
                file.delete()
                return@withContext DownloadResult.Failure("O arquivo baixado está corrompido. Tente novamente.")
            }
            prefs.edit { putString(KEY_DOWNLOADED_VERSION, update.latestVersion) }
            DownloadResult.Success(file)
        } catch (e: Exception) {
            file.delete()
            DownloadResult.Failure("Falha ao baixar a atualização. Verifique sua conexão.")
        }
    }

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun installApk(file: File) {
        if (!file.exists()) return
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TerriFlow-App")
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

    private fun followRedirects(initial: HttpURLConnection): HttpURLConnection {
        var connection = initial
        repeat(MAX_REDIRECTS) {
            if (connection.responseCode !in REDIRECT_CODES) return connection
            val location = connection.getHeaderField("Location") ?: return connection
            connection.disconnect()
            connection = open(URL(connection.url, location).toString())
        }
        return connection
    }

    private companion object {
        const val RELEASES_API_URL = "https://api.github.com/repos/MRaphaelpy/TerriFlow/releases/latest"
        const val APK_NAME = "terriflow-update.apk"
        const val KEY_SNOOZED_VERSION = "dismissed_version"
        const val KEY_SNOOZED_UNTIL = "snoozed_until"
        const val KEY_DOWNLOADED_VERSION = "downloaded_version"
        const val SNOOZE_HOURS = 24L
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_REDIRECTS = 5
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
