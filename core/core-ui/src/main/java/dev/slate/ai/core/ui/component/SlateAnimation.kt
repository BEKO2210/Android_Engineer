package dev.slate.ai.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

object SlateTransitions {
    val fadeIn: EnterTransition = fadeIn(animationSpec = tween(200))
    val fadeOut: ExitTransition = fadeOut(animationSpec = tween(200))

    val scaleIn: EnterTransition = scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(200),
    )
    val scaleOut: ExitTransition = scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(200),
    )

    val slideInFromBottom: EnterTransition = slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(300),
    ) + fadeIn(animationSpec = tween(300))

    val slideOutToBottom: ExitTransition = slideOutVertically(
        targetOffsetY = { it / 4 },
        animationSpec = tween(300),
    ) + fadeOut(animationSpec = tween(300))

    val expandVertical: EnterTransition = expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = tween(200),
    ) + fadeIn(animationSpec = tween(200))

    val shrinkVertical: ExitTransition = shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = tween(200),
    ) + fadeOut(animationSpec = tween(200))
}

@Composable
fun SlateFadeIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = SlateTransitions.fadeIn,
        exit = SlateTransitions.fadeOut,
    ) {
        content()
    }
}

@Composable
fun SlateSlideIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = SlateTransitions.slideInFromBottom,
        exit = SlateTransitions.slideOutToBottom,
    ) {
        content()
    }
}
