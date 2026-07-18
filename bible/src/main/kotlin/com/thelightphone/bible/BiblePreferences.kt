package com.thelightphone.bible

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object BiblePreferences {
    val API_KEY = stringPreferencesKey("esv_api_key")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val FONT_SIZE = stringPreferencesKey("font_size")
    val LINE_SPACING = stringPreferencesKey("line_spacing")
    val VERSE_NUMBERS = booleanPreferencesKey("verse_numbers")
    val LAST_BOOK_IDX = intPreferencesKey("last_book_idx")
    val LAST_CHAPTER = intPreferencesKey("last_chapter")
}

enum class FontSize(val label: String, val bodyPx: Float) {
    S("Small", 32f), M("Medium", 38f), L("Large", 44f), XL("Extra Large", 52f);

    companion object {
        fun fromStorage(value: String?): FontSize = entries.firstOrNull { it.name == value } ?: M
    }
}

enum class LineSpacing(val label: String, val multiplier: Float) {
    Compact("Compact", 1.32f), Normal("Normal", 1.55f), Relaxed("Relaxed", 1.8f);

    companion object {
        fun fromStorage(value: String?): LineSpacing = entries.firstOrNull { it.name == value } ?: Normal
    }
}

data class BibleSettings(
    val apiKey: String = "",
    val darkMode: Boolean = true,
    val fontSize: FontSize = FontSize.M,
    val lineSpacing: LineSpacing = LineSpacing.Normal,
    val verseNumbers: Boolean = true,
)
