package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    focusBorderColor: Color = MazzeCyan,
    defaultBorderColor: Color = Color.Transparent,
    focusBorderWidth: Dp = 2.5.dp,
    defaultBorderWidth: Dp = 0.dp,
    scaleOnFocus: Float = 1.035f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "tv_focus_scale"
    )

    var mod = this
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }
        .scale(scale)
        .border(
            width = if (isFocused) focusBorderWidth else defaultBorderWidth,
            color = if (isFocused) focusBorderColor else defaultBorderColor,
            shape = shape
        )

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        mod = mod.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        mod = mod.focusable(enabled = enabled)
    }

    mod
}
