package com.auradock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    emptyStateText: String = "No events today"
) {
    val spacing = LocalAuraDockSpacing.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AuraDockColors.Surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = title,
                style = AuraDockTypography.WidgetPrimary,
                color = AuraDockColors.OnBackground
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            if (items.isEmpty()) {
                Text(
                    text = emptyStateText,
                    style = AuraDockTypography.WidgetLabel,
                    color = AuraDockColors.OnSurfaceVariant
                )
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TodaySnapshotCardEmptyPreview() {
    AuraDockTheme {
        TodaySnapshotCard(items = emptyList())
    }
}
