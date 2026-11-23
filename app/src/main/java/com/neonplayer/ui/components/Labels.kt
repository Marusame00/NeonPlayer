package com.neonplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Neon.palette.textSecondary,
    dotColor: Color? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(6.dp)
                    .neonGlow(dotColor, radius = 6.dp, cornerRadius = 3.dp, alpha = 0.75f)
                    .background(dotColor, CircleShape),
            )
        }
        Text(
            text = text.uppercase(),
            style = NeonType.Label,
            color = color,
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Neon.palette.primary,
) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = NeonType.MonoSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = Neon.palette.primary,
) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.4f), CircleShape)
            .background(color.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    ) {
        Text(
            text = count.toString(),
            style = NeonType.MonoSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}
