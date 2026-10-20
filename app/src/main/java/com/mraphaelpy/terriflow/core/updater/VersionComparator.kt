package com.mraphaelpy.terriflow.core.updater

/** Compara versões no formato "1.2.3" (aceita prefixo "v" e sufixos como "-beta"). */
object VersionComparator {

    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote = numericParts(remoteTag)
        val current = numericParts(currentVersion)
        if (remote.isEmpty()) return false

        for (i in 0 until maxOf(remote.size, current.size)) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    fun clean(version: String): String = version.trim().removePrefix("v").removePrefix("V")

    private fun numericParts(version: String): List<Int> =
        clean(version).split(".", "-", "_").mapNotNull { it.toIntOrNull() }
}
