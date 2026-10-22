package com.auradock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.LocalAuraDockSpacing

@Composable
fun GreetingHeader(
    userName: String,
    dateText: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalAuraDockSpacing.current

    Column(modifier = modifier) {
        Text(
            text = "Good Morning, $userName",
            style = AuraDockTypography.WidgetPrimary,
            color = AuraDockColors.OnBackground
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = dateText,
            style = AuraDockTypography.WidgetLabel,
            color = AuraDockColors.OnSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GreetingHeaderPreview() {
    AuraDockTheme {
        GreetingHeader(
            userName = "Albin",
            dateText = "Tuesday \u2022 March 12"
        )
    }
}
