package com.mraphaelpy.terriflow.core.updater

object ReleaseNotes {

    fun clean(body: String): String = body.lines()
        .map { it.trim() }
        .filterNot { it.isEmpty() || it.startsWith("**Full Changelog**") || it.startsWith("## ") }
        .map { line ->
            line.removePrefix("- ").removePrefix("* ")
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                .replace(Regex("\\[(.+?)]\\(.+?\\)"), "$1")
                .replace(Regex("\\s+by @\\S+ in https?://\\S+"), "")
                .let { "• $it" }
        }
        .joinToString("\n")
        .ifBlank { "Melhorias gerais e correções." }
}
