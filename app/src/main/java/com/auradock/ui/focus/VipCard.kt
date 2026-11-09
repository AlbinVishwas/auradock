package com.auradock.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.auradock.focus.vip.MAX_VIPS
import com.auradock.focus.vip.Vip
import com.auradock.focus.vip.canAddVip
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.components.AuraSwitch

/** What the "People who can always reach you" card shows. */
data class VipUiState(
    val vips: List<Vip> = emptyList(),
    /** Calls from them ring through Do Not Disturb while docked (on only if the permission to do it is also granted). */
    val ringThrough: Boolean = false,
    /** How many people are marked right now only because AuraDock lent the mark (normally 0 outside a dock). */
    val onLoan: Int = 0
)

/** What the user can do on the card. */
data class VipActions(
    val onAdd: () -> Unit = {},
    val onRemove: (Vip) -> Unit = {},
    val onToggleRingThrough: (Boolean) -> Unit = {},
    val onPutBack: () -> Unit = {}
)

/**
 * "People who can always reach you": up to five contacts, a switch for letting their calls ring through, and, if AuraDock is
 * still holding a loaned mark (after a crash, say), a button to put it back. Self-contained: it takes only [state] and [actions].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VipCard(state: VipUiState, actions: VipActions, modifier: Modifier = Modifier) {
    FocusCardFrame(modifier) {
        Text(text = "People who can always reach you", style = AuraDockTypography.CardTitle, color = AuraDockColors.OnBackground)
        Text(
            text = "Up to $MAX_VIPS. Their calls ring through, and their messages count as let through on the dock.",
            style = AuraDockTypography.Meta,
            color = AuraDockColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = AuraDockSpacing.xs)
        )
        if (state.vips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
            ) { state.vips.forEach { VipChip(it, onRemove = { actions.onRemove(it) }) } }
        }
        CardDivider(Modifier.padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs))
        val canAdd = canAddVip(state.vips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canAdd, role = Role.Button, onClick = actions.onAdd)
                .padding(vertical = AuraDockSpacing.smd + AuraDockSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (canAdd) "Add a person" else "Remove someone to add another",
                style = AuraDockTypography.RowLabel,
                color = if (canAdd) AuraDockColors.OnBackground else AuraDockColors.Tertiary,
                modifier = Modifier.weight(1f)
            )
            Text(text = "${state.vips.size} of $MAX_VIPS", style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant)
            if (canAdd) {
                Icon(
                    Icons.Rounded.ChevronRight, contentDescription = null, tint = AuraDockColors.Tertiary,
                    modifier = Modifier.padding(start = AuraDockSpacing.smd).size(AuraDockSizes.rowChevron)
                )
            }
        }
        CardDivider()
        Row(
            modifier = Modifier.padding(top = AuraDockSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(text = "Let their calls ring through", style = AuraDockTypography.RowLabel, color = AuraDockColors.OnBackground)
                Text(
                    text = "Do Not Disturb has no list of its own. While you are docked, AuraDock adds them to your Favourites " +
                        "in Contacts, then takes them out again when you undock. Anyone who was already a Favourite stays one. " +
                        "Needs permission to change contacts.",
                    style = AuraDockTypography.Meta,
                    color = AuraDockColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = AuraDockSpacing.xs)
                )
            }
            AuraSwitch(checked = state.ringThrough, onCheckedChange = actions.onToggleRingThrough)
        }
        if (state.onLoan > 0) {
            CardDivider(Modifier.padding(top = AuraDockSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Still on loan", style = AuraDockTypography.RowLabel, color = AuraDockColors.OnBackground)
                    Text(
                        text = "${state.onLoan} ${if (state.onLoan == 1) "person is" else "people are"} still in your Favourites because of AuraDock.",
                        style = AuraDockTypography.RowCaption,
                        color = AuraDockColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = AuraDockSpacing.xxs)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(AuraDockSizes.smallPill)
                        .clip(RoundedCornerShape(AuraDockSizes.smallPill / 2))
                        .background(AuraDockColors.QuietPill)
                        .clickable(role = Role.Button, onClick = actions.onPutBack)
                        .padding(horizontal = AuraDockSpacing.md),
                    contentAlignment = Alignment.Center
                ) { Text(text = "Put back", style = AuraDockTypography.SmallAction, color = AuraDockColors.OnBackground) }
            }
        }
    }
}

/** A person: their name, and a cross to take them off the list. */
@Composable
private fun VipChip(vip: Vip, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(AuraDockSizes.appChip)
            .clip(RoundedCornerShape(AuraDockSizes.appChip / 2))
            .background(AuraDockColors.Field)
            .clickable(role = Role.Button, onClick = onRemove)
            .semantics { contentDescription = "Remove ${vip.name}" }
            .padding(start = AuraDockSpacing.md, end = AuraDockSpacing.smd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
    ) {
        Text(text = vip.name.ifBlank { "Contact" }, style = AuraDockTypography.ChipLabel, color = AuraDockColors.OnBackground)
        Icon(Icons.Rounded.Close, contentDescription = null, tint = AuraDockColors.Tertiary, modifier = Modifier.size(AuraDockSizes.rowChevron))
    }
}

@Composable
private fun FocusCardFrame(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.card))
            .background(AuraDockColors.Surface)
            .padding(AuraDockSpacing.mdl - AuraDockSpacing.xxs)
    ) { content() }
}

@Composable
private fun CardDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(AuraDockSizes.line).background(AuraDockColors.RowDivider))
}
