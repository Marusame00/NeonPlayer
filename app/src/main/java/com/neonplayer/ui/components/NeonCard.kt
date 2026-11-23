package com.neonplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonShapes

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    glow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = Neon.palette
    val shape = RoundedCornerShape(NeonShapes.CardRadius)
    Column(
        modifier = modifier
            .then(
                if (glow) {
                    Modifier.neonGlow(palette.glow, radius = 14.dp, cornerRadius = NeonShapes.CardRadius, alpha = 0.5f)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(palette.surface.copy(alpha = 0.82f), palette.surface.copy(alpha = 0.68f)),
                ),
            )
            .border(1.dp, palette.border, shape)
            .padding(contentPadding),
        content = content,
    )
}
