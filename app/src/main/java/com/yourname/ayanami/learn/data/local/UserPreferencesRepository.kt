package com.yourname.ayanami.learn.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class ActivityStatus(val key: String) {
    Online("online"),
    Idle("idle"),
    Offline("offline");

    companion object {
        fun fromKey(key: String?): ActivityStatus {
            return entries.firstOrNull { it.key == key } ?: Online
        }
    }
}

data class UserPreferences(
    val firebaseUid: String? = null,
    val email: String? = null,
    val displayName: String = "",
    val bio: String = "",
    val photoUri: String? = null,
    val nativeLanguage: NativeLanguage = NativeLanguage.Portuguese,
    val dailyGoalMinutes: Int = 15,
    val voiceReplies: Boolean = true,
    val speakingExercises: Boolean = true,
    val soundEffects: Boolean = true,
    val studyReminders: Boolean = true,
    val darkTheme: Boolean = true,
    val activityStatus: ActivityStatus = ActivityStatus.Online,
    val rememberActivityStatus: Boolean = false
) {
    val profileName: String
        get() = displayName.ifBlank { email?.substringBefore("@").orEmpty() }
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _preferences = MutableStateFlow(readPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun setAuthenticatedUser(firebaseUid: String?, fullName: String?, email: String?) {
        prefs.edit {
            putString(KEY_FIREBASE_UID, firebaseUid)
            putString(KEY_EMAIL, email)
            if (!fullName.isNullOrBlank()) putString(KEY_DISPLAY_NAME, fullName)
        }
        refresh()
    }

    fun updateProfile(displayName: String, bio: String, photoUri: Uri?) {
        prefs.edit {
            putString(KEY_DISPLAY_NAME, displayName.trim())
            putString(KEY_BIO, bio.trim())
            if (photoUri != null) putString(KEY_PHOTO_URI, photoUri.toString())
        }
        if (photoUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    photoUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        refresh()
    }

    fun updateNativeLanguage(language: NativeLanguage) {
        prefs.edit { putString(KEY_NATIVE_LANGUAGE, language.code) }
        refresh()
    }

    fun updateDailyGoal(minutes: Int) {
        prefs.edit { putInt(KEY_DAILY_GOAL, minutes.coerceIn(5, 60)) }
        refresh()
    }

    fun updateVoiceReplies(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VOICE_REPLIES, enabled) }
        refresh()
    }

    fun updateSpeakingExercises(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SPEAKING_EXERCISES, enabled) }
        refresh()
    }

    fun updateSoundEffects(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND_EFFECTS, enabled) }
        refresh()
    }

    fun updateStudyReminders(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_STUDY_REMINDERS, enabled) }
        refresh()
    }

    fun updateDarkTheme(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_THEME, enabled) }
        refresh()
    }

    fun updateActivityStatus(status: ActivityStatus, remember: Boolean) {
        prefs.edit {
            putString(KEY_ACTIVITY_STATUS, status.key)
            putBoolean(KEY_REMEMBER_STATUS, remember)
        }
        refresh()
    }

    fun resetSessionStatusIfNeeded() {
        val remember = prefs.getBoolean(KEY_REMEMBER_STATUS, false)
        if (!remember) {
            prefs.edit { putString(KEY_ACTIVITY_STATUS, ActivityStatus.Online.key) }
            refresh()
        }
    }

    private fun refresh() {
        _preferences.update { readPreferences() }
    }

    private fun readPreferences(): UserPreferences {
        return UserPreferences(
            firebaseUid = prefs.getString(KEY_FIREBASE_UID, null),
            email = prefs.getString(KEY_EMAIL, null),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            bio = prefs.getString(KEY_BIO, "").orEmpty(),
            photoUri = prefs.getString(KEY_PHOTO_URI, null),
            nativeLanguage = NativeLanguage.fromCode(
                prefs.getString(KEY_NATIVE_LANGUAGE, NativeLanguage.Portuguese.code)
            ),
            dailyGoalMinutes = prefs.getInt(KEY_DAILY_GOAL, 15),
            voiceReplies = prefs.getBoolean(KEY_VOICE_REPLIES, true),
            speakingExercises = prefs.getBoolean(KEY_SPEAKING_EXERCISES, true),
            soundEffects = prefs.getBoolean(KEY_SOUND_EFFECTS, true),
            studyReminders = prefs.getBoolean(KEY_STUDY_REMINDERS, true),
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
            activityStatus = ActivityStatus.fromKey(
                prefs.getString(KEY_ACTIVITY_STATUS, ActivityStatus.Online.key)
            ),
            rememberActivityStatus = prefs.getBoolean(KEY_REMEMBER_STATUS, false)
        )
    }

    private companion object {
        const val PREFS_NAME = "ayanami_preferences"
        const val KEY_FIREBASE_UID = "firebase_uid"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_BIO = "bio"
        const val KEY_PHOTO_URI = "photo_uri"
        const val KEY_NATIVE_LANGUAGE = "native_language"
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_VOICE_REPLIES = "voice_replies"
        const val KEY_SPEAKING_EXERCISES = "speaking_exercises"
        const val KEY_SOUND_EFFECTS = "sound_effects"
        const val KEY_STUDY_REMINDERS = "study_reminders"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_ACTIVITY_STATUS = "activity_status"
        const val KEY_REMEMBER_STATUS = "remember_activity_status"
    }
}
