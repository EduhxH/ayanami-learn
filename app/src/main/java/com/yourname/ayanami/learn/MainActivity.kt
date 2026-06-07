package com.yourname.ayanami.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.ui.screens.auth.AccountCreationScreen
import com.yourname.ayanami.learn.ui.screens.auth.LoginScreen
import com.yourname.ayanami.learn.ui.screens.chat.ChatScreen
import com.yourname.ayanami.learn.ui.screens.exercise.ExerciseSessionScreen
import com.yourname.ayanami.learn.ui.screens.home.HomeScreen
import com.yourname.ayanami.learn.ui.screens.settings.SettingsScreen
import com.yourname.ayanami.learn.ui.screens.voice.LiveVoiceScreen
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings
import com.yourname.ayanami.learn.ui.localization.appStrings
import com.yourname.ayanami.learn.ui.theme.AyanamiLearnTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository.resetSessionStatusIfNeeded()

        setContent {
            val preferences by userPreferencesRepository.preferences.collectAsState()
            AyanamiLearnTheme(
                darkTheme = preferences.darkTheme,
                dynamicColor = false
            ) {
                CompositionLocalProvider(LocalAppStrings provides preferences.nativeLanguage.appStrings()) {
                    val navController = rememberNavController()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "login"
                        ) {
                            composable("login") {
                                LoginScreen(navController = navController)
                            }
                            composable("account_creation") {
                                AccountCreationScreen(navController = navController)
                            }
                            composable("home") {
                                HomeScreen(
                                    onOpenExercise = { skillKey -> navController.navigate("exercise/$skillKey") },
                                    onOpenAiMode = { navController.navigate("ai_mode") },
                                    onOpenProfile = { navController.navigate("profile") },
                                    onOpenSettings = { navController.navigate("settings") },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("chat") {
                                ChatScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenVoice = { navController.navigate("voice_chat") },
                                    onOpenProfile = { navController.navigate("profile") }
                                )
                            }
                            composable("voice_chat") {
                                LiveVoiceScreen(onBack = { navController.popBackStack() })
                            }
                            composable("exercise/{skillKey}") { backStackEntry ->
                                ExerciseSessionScreen(
                                    skillKey = backStackEntry.arguments?.getString("skillKey").orEmpty(),
                                    onBack = { navController.popBackStack() },
                                    onOpenChatSupport = { navController.navigate("chat") },
                                    onOpenVoiceSupport = { navController.navigate("voice_chat") }
                                )
                            }
                            composable("ai_mode") {
                                com.yourname.ayanami.learn.ui.screens.ai.AiModeScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenChat = { navController.navigate("chat") },
                                    onOpenVoice = { navController.navigate("voice_chat") }
                                )
                            }
                            composable("profile") {
                                com.yourname.ayanami.learn.ui.screens.profile.ProfileScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
