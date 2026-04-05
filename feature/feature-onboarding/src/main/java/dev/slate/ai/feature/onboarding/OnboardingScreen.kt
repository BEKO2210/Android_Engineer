package dev.slate.ai.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.common.DeviceTier
import dev.slate.ai.core.common.formatFileSize
import dev.slate.ai.core.ui.component.SlatePrimaryButton
import dev.slate.ai.core.ui.component.SlateStatusChip
import dev.slate.ai.core.ui.component.SlateChipStyle
import dev.slate.ai.core.ui.component.SlateTextButton

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // App name
        Text(
            text = "Slate",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Private AI on your device",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))

        // Privacy promise
        InfoRow(
            icon = Icons.Default.Security,
            text = "Your data stays on this device. No cloud, no tracking.",
        )
        Spacer(Modifier.height(16.dp))
        InfoRow(
            icon = Icons.Default.PhoneAndroid,
            text = "AI models run locally using your phone's processor.",
        )

        Spacer(Modifier.height(32.dp))

        // Device assessment
        Text(
            text = "Your device",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DeviceStat(
                icon = Icons.Default.Memory,
                label = "RAM",
                value = "${state.totalRamMb / 1024} GB",
            )
            DeviceStat(
                icon = Icons.Default.Storage,
                label = "Free",
                value = state.availableStorageBytes.formatFileSize(),
            )
        }

        Spacer(Modifier.height(12.dp))

        SlateStatusChip(
            text = when (state.deviceTier) {
                DeviceTier.LOW -> "Limited device"
                DeviceTier.BASIC -> "Basic device"
                DeviceTier.STANDARD -> "Good device"
                DeviceTier.HIGH -> "Great device"
            },
            style = when (state.deviceTier) {
                DeviceTier.LOW -> SlateChipStyle.ERROR
                DeviceTier.BASIC -> SlateChipStyle.WARNING
                DeviceTier.STANDARD -> SlateChipStyle.INFO
                DeviceTier.HIGH -> SlateChipStyle.SUCCESS
            },
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = state.tierDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (state.deviceTier == DeviceTier.LOW) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Slate may not work well on this device. You can try, but expect slow performance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Recommended: ${state.recommendedModelName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(32.dp))

        SlatePrimaryButton(
            text = "Get started",
            onClick = {
                viewModel.completeOnboarding()
                onComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Start using Slate" },
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "You can download models from the Models tab.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeviceStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
