package com.neonplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun NeonChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    Row(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.neonGlow(palette.primary, radius = 10.dp, cornerRadius = 14.dp, alpha = 0.35f)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(
                if (selected) palette.primary.copy(alpha = 0.14f) else palette.surfaceHigh.copy(alpha = 0.55f),
                CircleShape,
            )
            .border(
                1.dp,
                if (selected) palette.primary.copy(alpha = 0.75f) else palette.textMuted.copy(alpha = 0.30f),
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = NeonType.Label,
            color = if (selected) palette.primary else palette.textSecondary,
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val palette = Neon.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = NeonType.Body, color = palette.textPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, style = NeonType.BodySmall, color = palette.textMuted)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(2.dp))
            trailing()
        }
    }
}
