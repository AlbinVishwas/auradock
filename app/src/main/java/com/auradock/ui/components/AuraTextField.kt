package com.auradock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/** The app's single-line text field (the task sheet's look): a Field-coloured bar, a white outline while focused, a grey hint when empty. */
@Composable
fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(AuraDockRadii.field)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = AuraDockTypography.FieldText.copy(color = AuraDockColors.OnBackground),
        cursorBrush = SolidColor(AuraDockColors.OnBackground),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .height(AuraDockSizes.field)
            .clip(shape)
            .background(AuraDockColors.Field)
            .border(AuraDockSizes.lineThick, if (focused) AuraDockColors.OnBackground else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = AuraDockSpacing.md), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(text = hint, style = AuraDockTypography.FieldText, color = AuraDockColors.Tertiary)
                inner()
            }
        }
    )
}
