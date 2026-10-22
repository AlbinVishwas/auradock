package com.auradock.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.auradock.ui.template.TemplateDefaults
import com.auradock.ui.theme.AuraDockTheme

@Preview(
    name = "AuraDock Template Preview",
    widthDp = 900,
    heightDp = 400,
    showBackground = false
)
@Composable
fun StandbyPreview() {
    AuraDockTheme {
        StandbyScreen(template = TemplateDefaults.Split, timeMillis = System.currentTimeMillis())
    }
}

@Preview(
    name = "AuraDock Minimal Preview",
    widthDp = 900,
    heightDp = 400,
    showBackground = false
)
@Composable
fun StandbyMinimalPreview() {
    AuraDockTheme {
        StandbyScreen(template = TemplateDefaults.Minimal, timeMillis = System.currentTimeMillis())
    }
}
