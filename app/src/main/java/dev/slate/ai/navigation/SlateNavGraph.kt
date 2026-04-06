package dev.slate.ai.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.slate.ai.feature.chat.ChatScreen
import dev.slate.ai.feature.models.ModelDetailScreen
import dev.slate.ai.feature.models.ModelsScreen
import dev.slate.ai.feature.onboarding.OnboardingScreen
import dev.slate.ai.feature.settings.ImportScreen
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

private const val FADE_MS = 200
private const val SLIDE_MS = 300

@Composable
fun SlateNavGraph(
    navController: NavHostController,
    isOnboardingComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = remember {
        if (isOnboardingComplete) TopLevelDestination.CHAT.route else "onboarding"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Smooth fade for top-level tab switches
        enterTransition = { fadeIn(tween(FADE_MS)) },
        exitTransition = { fadeOut(tween(FADE_MS)) },
        popEnterTransition = { fadeIn(tween(FADE_MS)) },
        popExitTransition = { fadeOut(tween(FADE_MS)) },
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
        // Detail screens slide in from right
        composable(
            route = "models/{modelId}",
            arguments = listOf(navArgument("modelId") { type = NavType.StringType }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) },
            exitTransition = { fadeOut(tween(FADE_MS)) },
            popEnterTransition = { fadeIn(tween(FADE_MS)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) },
        ) {
            ModelDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen(
                onNavigateToPrivacy = { navController.navigate("privacy") },
                onNavigateToStorage = { navController.navigate("storage") },
                onNavigateToImport = { navController.navigate("import") },
            )
        }
        composable(
            "privacy",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) },
        ) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "storage",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) },
        ) {
            StorageScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "import",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) },
        ) {
            ImportScreen(onBack = { navController.popBackStack() })
        }
    }
}
