package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.HomeUiState
import com.vjaykrsna.nanoai.ui.components.OfflineBanner
import com.vjaykrsna.nanoai.ui.components.OnboardingTooltip
import com.vjaykrsna.nanoai.ui.components.PrimaryActionCard
import kotlin.text.titlecase

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleTools: () -> Unit,
    onActionClick: (String) -> Unit,
    onTooltipDismiss: () -> Unit,
    onTooltipHelp: () -> Unit,
    onTooltipDontShow: () -> Unit,
    onRetryOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OfflineBanner(
                isOffline = state.offlineBannerVisible,
                queuedActions = state.queuedActions,
                onRetry = onRetryOffline,
                modifier = Modifier.testTag("offline_banner_container_wrapper"),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("home_recent_actions_header"),
                    )
                    IconButton(
                        onClick = onToggleTools,
                        modifier = Modifier.testTag("home_tools_toggle"),
                    ) {
                        Icon(
                            imageVector = if (state.toolsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle tools",
                        )
                    }
                }

                if (state.toolsExpanded) {
                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("home_tools_panel_expanded"),
                    )
                } else {
                    Text(
                        text = "Advanced tools hidden",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("home_tools_panel_collapsed"),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = state.isHydrating, enter = fadeIn(), exit = fadeOut()) {
                    HomeSkeleton(modifier = Modifier.testTag("home_skeleton_loader"))
                }

                AnimatedVisibility(visible = !state.isHydrating, enter = fadeIn(), exit = fadeOut()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("home_single_column_feed"),
                    ) {
                        itemsIndexed(state.recentActions) { index, action ->
                            PrimaryActionCard(
                                title = action.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                description = "Quick action",
                                tag = "home_recent_action_$index",
                                onClick = { onActionClick(action) },
                            )
                        }
                    }
                }

                if (state.latencyIndicatorVisible) {
                    Text(
                        text = "Response in under 100ms",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.testTag("home_latency_meter"),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.tooltipEntryVisible) {
                    Column(modifier = Modifier.fillMaxWidth().testTag("onboarding_tooltip_entry")) {
                        OnboardingTooltip(
                            message = "Tip: Pin your favorite tools for quick access.",
                            onDismiss = onTooltipDismiss,
                            onDontShowAgain = onTooltipDontShow,
                            onHelp = onTooltipHelp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth().alpha(0.5f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}
