package com.auradock.ui.components

import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockRadii
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auradock.ui.preview.PreviewComponent
import androidx.compose.ui.unit.dp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.LocalAuraDockSpacing

@Composable
fun TodaySnapshotCard(
    items: List<String>,
    modifier: Modifier = Modifier,
    title: String = "Today",
    emptyStateText: String = "No events today",
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    val spacing = LocalAuraDockSpacing.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AuraDockRadii.card),
        color = AuraDockColors.Surface,
        tonalElevation = AuraDockSizes.none
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = title,
                style = AuraDockTypography.WidgetPrimary,
                color = AuraDockColors.OnBackground
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            if (items.isEmpty()) {
                if (emptyStateText.isNotBlank()) Text(
                    text = emptyStateText,
                    style = AuraDockTypography.WidgetLabel,
                    color = AuraDockColors.OnSurfaceVariant
                )
                if (actionText != null) {
                    Spacer(modifier = Modifier.height(spacing.md))
                    QuickActionButton(text = actionText, onClick = onAction, isActive = true)
                }
            } else {
                items.forEachIndexed { index, item ->
                    Text(
                        text = item,
                        style = AuraDockTypography.WidgetLabel,
                        color = AuraDockColors.OnSurfaceVariant
                    )
                    if (index != items.lastIndex) {
                        Spacer(modifier = Modifier.height(spacing.xs))
                    }
                }
            }
        }
    }
}

@PreviewComponent
@Composable
private fun TodaySnapshotCardPreview() {
    AuraDockTheme {
        TodaySnapshotCard(
            items = listOf(
                "Team standup 10:30",
                "Investor call 14:00"
            )
        )
    }
}

@PreviewComponent
@Composable
private fun TodaySnapshotCardEmptyPreview() {
    AuraDockTheme {
        TodaySnapshotCard(items = emptyList())
    }
}
