package com.niru.ai

import android.content.Context

/** Sirf is phone me save hota hai. Koi data bahar nahi jata. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("niru_prefs", Context.MODE_PRIVATE)

    var character: String
        get() = sp.getString("character", CHAR_NIYA) ?: CHAR_NIYA
        set(v) { sp.edit().putString("character", v).apply() }

    var voiceMode: Boolean
        get() = sp.getBoolean("voice_mode", false)
        set(v) { sp.edit().putBoolean("voice_mode", v).apply() }

    /** Aapki apni Gemini key (Google AI Studio se). Production app me ye backend se jani chahiye. */
    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) { sp.edit().putString("api_key", v.trim()).apply() }

    var model: String
        get() = sp.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(v) {
            val clean = v.trim().filter { it.isLetterOrDigit() || it == '-' || it == '.' || it == '_' }
            sp.edit().putString("model", clean.ifBlank { DEFAULT_MODEL }).apply()
        }

    companion object {
        const val CHAR_NIYA = "niya"
        const val CHAR_ROCKY = "rocky"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}
