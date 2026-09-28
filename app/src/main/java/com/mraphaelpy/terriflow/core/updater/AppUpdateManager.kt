package com.mraphaelpy.terriflow.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.mraphaelpy.terriflow.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val GITHUB_OWNER = "MRaphaelpy"
        private const val GITHUB_REPO = "TerriFlow"
        private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    private val prefs = context.getSharedPreferences("terriflow_updater", Context.MODE_PRIVATE)

    fun isVersionDismissed(version: String): Boolean {
        val cleanVersion = version.removePrefix("v").trim()
        val dismissed = prefs.getString("dismissed_version", null)
        return dismissed != null && dismissed == cleanVersion
    }

    fun dismissVersion(version: String) {
        val cleanVersion = version.removePrefix("v").trim()
        prefs.edit().putString("dismissed_version", cleanVersion).apply()
    }

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(RELEASES_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "TerriFlow-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) return@runCatching null

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", tagName)
            val changelog = json.optString("body", "Melhorias gerais e correções de bugs.")

            var downloadUrl = ""
            var apkSize = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (downloadUrl.isEmpty()) return@runCatching null

            val currentVersion = runCatching {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                pInfo.versionName?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME
            }.getOrDefault(BuildConfig.VERSION_NAME)

            val isNewer = isNewerVersion(tagName, currentVersion)

            AppUpdateInfo(
                hasUpdate = isNewer,
                currentVersion = currentVersion,
                latestVersion = tagName.removePrefix("v"),
                releaseTitle = title,
                changelog = changelog,
                downloadUrl = downloadUrl,
                apkSize = apkSize
            )
        }.getOrNull()
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "TerriFlow-App")
                connectTimeout = 15000
                readTimeout = 30000
            }

            // Segue redirects automáticos do GitHub (S3/assets)
            val finalConnection = if (connection.responseCode in listOf(301, 302, 303, 307, 308)) {
                val newUrl = connection.getHeaderField("Location")
                (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "TerriFlow-App")
                }
            } else {
                connection
            }

            val totalBytes = finalConnection.contentLengthLong
            val updateDir = File(context.getExternalFilesDir(null), "Download").apply { mkdirs() }
            val apkFile = File(updateDir, "terriflow-update.apk")
            if (apkFile.exists()) apkFile.delete()

            finalConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        if (totalBytes > 0) {
                            val progress = totalDownloaded.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1f)
            }
            apkFile
        }.getOrNull()
    }

    fun installApk(file: File) {
        if (!file.exists()) return

        // No Android 8+, verifica permissão de instalar apps de fontes desconhecidas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    fun isNewerVersion(remoteTag: String, currentVer: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanCurrent = currentVer.removePrefix("v").trim()
        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) return false

        val remoteParts = cleanRemote.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".", "-", "_").mapNotNull { it.toIntOrNull() }

        if (remoteParts.isNotEmpty() && remoteParts == currentParts) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
