package com.auradock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.auradock.dock.world.WorldCities
import com.auradock.dock.world.WorldCity
import com.auradock.ui.components.AuraTextField
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockWorld

/** Under the World card: "Cities" and the ones picked (or "Add up to 2"), tapped to open the picker. */
@Composable
fun WorldCitiesRow(cities: List<WorldCity>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.row))
            .background(AuraDockColors.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AuraDockSpacing.md, vertical = AuraDockSpacing.smd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("More cities", color = AuraDockColors.OnBackground, style = AuraDockTypography.RowTitle)
        Text(
            if (cities.isEmpty()) "Add up to ${AuraDockWorld.MaxExtraCities}" else cities.joinToString(", ") { it.short },
            color = AuraDockColors.OnSurfaceVariant,
            style = AuraDockTypography.Secondary
        )
    }
}

/** The searchable list of cities: tap to add (up to two) or remove. Saved as each one is tapped. */
@Composable
fun WorldCitiesSheetContent(selected: List<WorldCity>, onChange: (List<WorldCity>) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { WorldCities.search(query) }
    Column(
        modifier = Modifier.padding(horizontal = AuraDockGallery.cardPadding).padding(bottom = AuraDockSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        Text("More cities", color = AuraDockColors.OnBackground, style = AuraDockTypography.SheetTitle)
        Text(
            "Up to ${AuraDockWorld.MaxExtraCities} cities show on the map with their local time.",
            color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary
        )
        AuraTextField(value = query, onValueChange = { query = it }, hint = "Search cities")
        LazyColumn(modifier = Modifier.heightIn(max = AuraDockGallery.cardHeight * 2)) {
            items(results, key = { it.name }) { city ->
                val on = selected.any { it.name == city.name }
                val full = !on && selected.size >= AuraDockWorld.MaxExtraCities
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (full) AuraDockGallery.DimmedAlpha else 1f)
                        .clickable(enabled = !full, role = Role.Checkbox) {
                            onChange(if (on) selected.filter { it.name != city.name } else selected + city)
                        }
                        .padding(vertical = AuraDockSpacing.smd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(city.name, color = AuraDockColors.OnBackground, style = AuraDockTypography.RowTitle)
                        Text(city.zoneId.replace('_', ' '), color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.RowCaption)
                    }
                    if (on) Icon(Icons.Rounded.Check, contentDescription = "Picked", tint = AuraDockColors.Accent, modifier = Modifier.size(AuraDockSizes.icon))
                    else Box(Modifier.size(AuraDockSizes.icon))
                }
            }
        }
    }
}
