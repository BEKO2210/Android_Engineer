package dev.slate.ai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.slate.ai.core.ui.component.SlateCard
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
            // Hero statement
            Text(
                text = "Your data never leaves your device.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Slate is designed from the ground up to protect your privacy. No data collection, no tracking, no cloud processing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // What we DO and DON'T do
            SlateCard {
                Text("What Slate does", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                CheckItem(true, "Runs AI models entirely on your device")
                CheckItem(true, "Stores conversations locally in app-private storage")
                CheckItem(true, "Uses HTTPS encryption for model downloads")
                CheckItem(true, "Lets you delete all data at any time")
                CheckItem(true, "Works fully offline after model download")
            }

            Spacer(Modifier.height(12.dp))

            SlateCard {
                Text("What Slate does NOT do", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                CheckItem(false, "Send your conversations to any server")
                CheckItem(false, "Collect analytics or usage data")
                CheckItem(false, "Use advertising identifiers")
                CheckItem(false, "Include crash reporting services")
                CheckItem(false, "Track your behavior across apps")
                CheckItem(false, "Require an account or login")
                CheckItem(false, "Share any data with third parties")
                CheckItem(false, "Make background network requests")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))

            // Detailed sections
            PrivacySection(
                title = "Local data storage",
                body = "All data is stored in Android's app-private directories. Other apps cannot access this data. When you uninstall Slate, all data is automatically and permanently deleted by the operating system.\n\nStored data includes:\n• Downloaded AI model files\n• Chat conversations (if enabled in Settings)\n• Your app preferences"
            )

            PrivacySection(
                title = "Network access",
                body = "Slate connects to the internet exclusively to download AI model files from Hugging Face (huggingface.co). Downloads use HTTPS encryption. No other network requests are made at any time. You can verify this by enabling the 'Offline only' mode in Settings — the app works fully offline after downloading a model."
            )

            PrivacySection(
                title = "No telemetry",
                body = "Slate ships with zero telemetry. There is no opt-in, no opt-out, because there is nothing to opt into. No usage data, no performance metrics, no error reports are collected or transmitted. If telemetry is ever introduced in a future version, it will be clearly announced, require explicit consent, and remain off by default."
            )

            PrivacySection(
                title = "Custom model imports",
                body = "When you import a custom .gguf model file, it is copied to Slate's private storage. The original file on your device is not modified or deleted. Slate does not upload, transmit, or share imported models."
            )

            PrivacySection(
                title = "Your controls",
                body = "• Disable 'Save chat history' to prevent conversation storage\n• Delete individual conversations from the chat screen\n• Clear all chat history from Settings → Data\n• Delete downloaded models from Settings → Storage\n• Uninstall the app to permanently remove all data"
            )

            PrivacySection(
                title = "Open source",
                body = "Slate's source code is available for public inspection. You can verify that the app behaves exactly as described in this policy by reviewing the code yourself."
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Contact",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "belkis.aslani@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
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
private fun CheckItem(positive: Boolean, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (positive) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (positive) Color(0xFF81C784) else Color(0xFFCF6679),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
    )
    Spacer(Modifier.height(20.dp))
}
