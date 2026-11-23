package com.neonplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonShapes
import com.neonplayer.ui.theme.NeonType

@Composable
fun NeonSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val shape = RoundedCornerShape(NeonShapes.SmallRadius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = NeonType.Body.copy(color = palette.textPrimary),
        cursorBrush = SolidColor(palette.primary),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.base1.copy(alpha = 0.85f), shape)
            .border(1.dp, palette.border, shape),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = NeonType.Body,
                            color = palette.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onValueChange("") },
                    )
                }
            }
        },
    )
}

@Composable
fun <T> NeonDropdown(
    selected: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NeonShapes.SmallRadius)
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(palette.surfaceHigh.copy(alpha = 0.70f), shape)
                .border(1.dp, palette.border, shape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label(selected), style = NeonType.Body, color = palette.textPrimary)
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = palette.surfaceHigh,
            shape = RoundedCornerShape(NeonShapes.SmallRadius),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label(option),
                            style = NeonType.Body,
                            color = if (option == selected) palette.primary else palette.textPrimary,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun NeonTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(index) }
                    .drawBehind {
                        if (selected) {
                            val y = size.height - 1.dp.toPx()
                            drawLine(
                                color = palette.primary.copy(alpha = 0.35f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = palette.primary,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    .padding(top = 6.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tab.uppercase(),
                    style = NeonType.Label,
                    color = if (selected) palette.primary else palette.textSecondary,
                )
            }
        }
    }
}

@Composable
fun NeonTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
) {
    val palette = Neon.palette
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (primary) {
                    Modifier.background(
                        Brush.horizontalGradient(listOf(palette.primary, palette.primary.copy(alpha = 0.82f))),
                        CircleShape,
                    )
                } else {
                    Modifier.background(palette.surfaceHigh.copy(alpha = 0.55f), CircleShape)
                },
            )
            .border(1.dp, if (primary) Color.Transparent else palette.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (primary) palette.base0 else palette.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            style = NeonType.Label,
            color = if (primary) palette.base0 else palette.textPrimary,
        )
    }
}
