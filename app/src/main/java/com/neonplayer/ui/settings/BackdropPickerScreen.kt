package com.neonplayer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.domain.model.BACKDROP_CUSTOM_PREFIX
import com.neonplayer.ui.backdrop.BackdropCatalog
import com.neonplayer.ui.backdrop.BackdropOption
import com.neonplayer.ui.backdrop.BackdropThumbnail
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonSlider
import com.neonplayer.ui.components.SectionLabel
import com.neonplayer.ui.components.gridTexture
import com.neonplayer.ui.components.neonGlow
import com.neonplayer.ui.main.MainViewModel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun BackdropPickerScreen(
    vm: MainViewModel,
    onClose: () -> Unit,
) {
    val palette = Neon.palette
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val options = remember(settings.backdropId, settings.customBackdrops) {
        BackdropCatalog.list(context, settings.customBackdrops)
    }
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.addCustomBackdrop(uri) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.base0.copy(alpha = 0.92f))
            .gridTexture(palette.primary.copy(alpha = 0.035f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeonIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Close backdrops",
                    onClick = onClose,
                    size = 34.dp,
                )
                SectionLabel(text = "Backdrops", color = palette.primary)
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(200.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = options, key = { it.id }) { option ->
                    BackdropTile(
                        option = option,
                        selected = option.id == settings.backdropId,
                        onSelect = { vm.updateSettings { it.copy(backdropId = option.id) } },
                        onDelete = if (option.id.startsWith(BACKDROP_CUSTOM_PREFIX)) {
                            {
                                vm.removeCustomBackdrop(
                                    option.id.removePrefix(BACKDROP_CUSTOM_PREFIX),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
                item(key = "add") {
                    AddBackdropTile(
                        onClick = { videoLauncher.launch(arrayOf("video/*")) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dim ${(settings.backdropDim * 100f).toInt()}%",
                style = NeonType.BodySmall,
                color = palette.textSecondary,
            )
            NeonSlider(
                value = settings.backdropDim,
                onValueChange = { value -> vm.updateSettings { it.copy(backdropDim = value) } },
                valueRange = 0f..0.8f,
            )
            Text(
                text = "Drop your own looping MP4s (H.264/HEVC, 720p–1080p, 30fps) into " +
                    "app/src/main/assets/backdrops and rebuild — they appear here automatically.",
                style = NeonType.BodySmall,
                color = palette.textMuted,
            )
        }
    }
}

@Composable
private fun BackdropTile(
    option: BackdropOption,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val palette = Neon.palette
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.neonGlow(palette.primary, radius = 14.dp, cornerRadius = 14.dp, alpha = 0.45f)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.70f), shape)
            .border(1.dp, if (selected) palette.primary else palette.border, shape)
            .clickable(onClick = onSelect)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            BackdropThumbnail(option = option, modifier = Modifier.fillMaxSize())
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .background(palette.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = palette.base0,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                ) {
                    NeonIconButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Remove backdrop",
                        onClick = onDelete,
                        size = 26.dp,
                        tint = palette.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = option.label,
            style = NeonType.BodySmall,
            color = if (selected) palette.primary else palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddBackdropTile(onClick: () -> Unit) {
    val palette = Neon.palette
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surfaceHigh.copy(alpha = 0.40f), shape)
            .drawBehind {
                drawRoundRect(
                    color = palette.textMuted.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add my own video",
            style = NeonType.BodySmall,
            color = palette.textSecondary,
        )
    }
}
