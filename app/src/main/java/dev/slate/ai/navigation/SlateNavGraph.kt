package dev.slate.ai.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.slate.ai.core.datastore.SlatePreferences
import dev.slate.ai.feature.chat.ChatScreen
import dev.slate.ai.feature.models.ModelDetailScreen
import dev.slate.ai.feature.models.ModelsScreen
import dev.slate.ai.feature.onboarding.OnboardingScreen
import dev.slate.ai.feature.settings.PrivacyScreen
import dev.slate.ai.feature.settings.SettingsScreen
import dev.slate.ai.feature.settings.StorageScreen

enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
) {
    CHAT(route = "chat", icon = Icons.Default.ChatBubble, label = "Chat"),
    MODELS(route = "models", icon = Icons.Default.Download, label = "Models"),
    SETTINGS(route = "settings", icon = Icons.Default.Settings, label = "Settings"),
}

@Composable
fun SlateNavGraph(
    navController: NavHostController,
    preferences: SlatePreferences,
    modifier: Modifier = Modifier,
) {
    val isOnboardingComplete by preferences.isOnboardingComplete.collectAsState(initial = true)

    val startDestination = if (isOnboardingComplete) {
        TopLevelDestination.CHAT.route
    } else {
        "onboarding"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(TopLevelDestination.CHAT.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }
        composable(TopLevelDestination.CHAT.route) {
            ChatScreen()
        }
        composable(TopLevelDestination.MODELS.route) {
            ModelsScreen(
                onModelClick = { modelId ->
                    navController.navigate("models/$modelId")
                },
            )
        }
        composable(
            route = "models/{modelId}",
            arguments = listOf(navArgument("modelId") { type = NavType.StringType }),
        ) {
            ModelDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen(
                onNavigateToPrivacy = { navController.navigate("privacy") },
                onNavigateToStorage = { navController.navigate("storage") },
            )
        }
        composable("privacy") {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable("storage") {
            StorageScreen(onBack = { navController.popBackStack() })
        }
    }
}
