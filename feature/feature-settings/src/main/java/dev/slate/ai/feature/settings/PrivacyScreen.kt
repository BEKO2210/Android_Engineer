package dev.slate.ai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slate.ai.core.ui.component.SlateTopBar

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SlateTopBar(
            title = "Privacy",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            PrivacySection(
                title = "Your data stays on your device",
                body = "Slate runs AI models entirely on your phone. Your conversations, prompts, and responses are never sent to any server. There is no cloud processing."
            )

            PrivacySection(
                title = "What Slate stores locally",
                body = """Slate stores the following data on your device only:

• Downloaded AI model files (in app-private storage)
• Chat conversations (if chat history is enabled)
• App preferences (theme, settings)

All data is stored in app-private directories that other apps cannot access. When you uninstall Slate, all data is automatically deleted."""
            )

            PrivacySection(
                title = "Network access",
                body = "Slate connects to the internet only to download AI model files from Hugging Face. These downloads use HTTPS encryption. No other network requests are made — no analytics, no tracking, no telemetry."
            )

            PrivacySection(
                title = "No tracking",
                body = "Slate contains no analytics SDKs, no advertising identifiers, no crash reporting services, and no third-party tracking code. We do not collect, transmit, or sell any user data."
            )

            PrivacySection(
                title = "No accounts",
                body = "Slate does not require an account, login, or registration. There is no user identifier associated with your usage."
            )

            PrivacySection(
                title = "Your controls",
                body = """You have full control over your data:

• Disable chat history in Settings to prevent conversation storage
• Delete individual conversations from the chat screen
• Clear all chat history from Settings
• Delete downloaded models from the Models screen
• Uninstall the app to remove all data completely"""
            )

            PrivacySection(
                title = "Open source",
                body = "Slate's source code is available for inspection. You can verify that the app behaves exactly as described in this policy."
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Contact: belkis.aslani@gmail.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Last updated: April 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
}
