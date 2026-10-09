package com.yshaw.myprinter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A compact single-line input.
 *
 * Material's OutlinedTextField enforces a 56dp minimum height with 16dp of internal vertical
 * padding; forcing a smaller height clips the text (letters like q, y, p, g, j lost their
 * descenders). This field draws its own outline instead, so it can be shorter while the text keeps
 * the full line height it needs.
 */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val strokeColor = if (focused) Primary else FieldStroke
    val strokeWidth = if (focused) 1.5.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
        cursorBrush = SolidColor(Primary),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(SurfaceColor)
            .border(strokeWidth, strokeColor, shape)
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, fontSize = 13.sp, color = HintColor)
                    }
                    innerTextField()
                }
                if (trailing != null) {
                    trailing()
                }
            }
        },
    )
}
