package com.jdrms.bulletin.domain.profile.infrastructure

import android.content.Context
import android.net.Uri

/** Migration from the pinned 3.1.4 SDK's default unencrypted Settings store. */
internal class LegacySupabaseSession(context: Context, projectUrl: String) {
    private val preferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    private val hostKey = Uri.parse(projectUrl).host?.replace('.', '-')

    fun read(): String? = keys().firstOrNull { it == "session" || it.endsWith("-session") }
        ?.let { preferences.getString(it, null) }

    fun clear() {
        val editor = preferences.edit()
        keys().forEach(editor::remove)
        check(editor.commit()) { "Unable to remove legacy authentication." }
    }

    private fun keys(): List<String> = preferences.all.keys.filter { key ->
        key == "session" || key == "supabase_code_verifier" ||
            (
                hostKey != null && key.startsWith("sb-") && hostKey in key &&
                    (key.endsWith("-session") || key.endsWith("-supabase_code_verifier"))
                )
    }
}
