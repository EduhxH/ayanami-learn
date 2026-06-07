package com.yourname.ayanami.learn.data.local

enum class NativeLanguage(
    val code: String,
    val englishLabel: String
) {
    Portuguese(code = "pt", englishLabel = "Portuguese"),
    Ukrainian(code = "uk", englishLabel = "Ukrainian"),
    Russian(code = "ru", englishLabel = "Russian");

    fun displayName(appLanguage: NativeLanguage): String {
        return when (appLanguage) {
            Portuguese -> when (this) {
                Portuguese -> "Português"
                Ukrainian -> "Ucraniano"
                Russian -> "Russo"
            }
            Ukrainian -> when (this) {
                Portuguese -> "Португальська"
                Ukrainian -> "Українська"
                Russian -> "Російська"
            }
            Russian -> when (this) {
                Portuguese -> "Португальский"
                Ukrainian -> "Украинский"
                Russian -> "Русский"
            }
        }
    }

    companion object {
        fun fromCode(code: String?): NativeLanguage {
            return entries.firstOrNull { it.code == code } ?: Portuguese
        }
    }
}
