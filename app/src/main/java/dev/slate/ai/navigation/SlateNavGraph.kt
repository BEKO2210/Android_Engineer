package dev.slate.ai.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.slate.ai.feature.chat.ChatScreen
import dev.slate.ai.feature.models.ModelsScreen
import dev.slate.ai.feature.settings.SettingsScreen

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
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.CHAT.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.CHAT.route) {
            ChatScreen()
        }
        composable(TopLevelDestination.MODELS.route) {
            ModelsScreen()
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
