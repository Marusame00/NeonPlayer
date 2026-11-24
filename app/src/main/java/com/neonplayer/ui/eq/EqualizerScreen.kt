package com.neonplayer.ui.eq

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.playback.EqUiState
import com.neonplayer.ui.components.NeonCard
import com.neonplayer.ui.components.NeonChip
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonSlider
import com.neonplayer.ui.components.NeonSwitch
import com.neonplayer.ui.components.NeonVerticalSlider
import com.neonplayer.ui.components.PillBadge
import com.neonplayer.ui.components.SectionLabel
import com.neonplayer.ui.components.gridTexture
import com.neonplayer.ui.main.MainViewModel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun EqualizerScreen(
    vm: MainViewModel,
    onClose: () -> Unit,
) {
    val palette = Neon.palette
    val state by vm.eqState.collectAsStateWithLifecycle()

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
                    contentDescription = stringResource(R.string.eq_close),
                    onClick = onClose,
                    size = 34.dp,
                )
                SectionLabel(text = stringResource(R.string.label_equalizer), color = palette.primary)
                Spacer(Modifier.weight(1f))
                PillBadge(text = state.route.uppercase())
                Spacer(Modifier.width(2.dp))
                NeonSwitch(
                    checked = state.enabled,
                    onCheckedChange = vm::setEqEnabled,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (!state.available) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.eq_unavailable),
                            style = NeonType.TitleM,
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.eq_start_playback),
                            style = NeonType.BodySmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.presets.forEach { preset ->
                            NeonChip(
                                text = preset,
                                selected = state.preset == preset,
                                onClick = { vm.applyEqPreset(preset) },
                            )
                        }
                    }
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        BandSliders(state = state, onBandChange = vm::setEqBand)
                    }
                    if (state.bassBoostSupported) {
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.eq_bass_boost, state.bassBoost / 10),
                                style = NeonType.BodySmall,
                                color = palette.textSecondary,
                            )
                            NeonSlider(
                                value = state.bassBoost.toFloat(),
                                onValueChange = { value -> vm.setBassBoost(value.toInt()) },
                                valueRange = 0f..1000f,
                                enabled = state.enabled,
                            )
                        }
                    }
                    if (state.virtualizerSupported) {
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.eq_virtualizer, state.virtualizer / 10),
                                style = NeonType.BodySmall,
                                color = palette.textSecondary,
                            )
                            NeonSlider(
                                value = state.virtualizer.toFloat(),
                                onValueChange = { value -> vm.setVirtualizer(value.toInt()) },
                                valueRange = 0f..1000f,
                                enabled = state.enabled,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.eq_saved_per_output),
                style = NeonType.BodySmall,
                color = palette.textMuted,
            )
        }
    }
}

@Composable
private fun BandSliders(
    state: EqUiState,
    onBandChange: (Int, Int) -> Unit,
) {
    val palette = Neon.palette
    val alpha = if (state.enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.bandFreqsHz.forEachIndexed { index, freq ->
            val level = state.levelsMb.getOrNull(index) ?: 0
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "%+.1f".format(level / 100f),
                    style = NeonType.MonoSmall,
                    color = palette.primary,
                )
                NeonVerticalSlider(
                    value = level.toFloat(),
                    onValueChange = { value ->
                        if (state.enabled) onBandChange(index, value.toInt())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    valueRange = state.minMb.toFloat()..state.maxMb.toFloat(),
                    centerLine = true,
                )
                Text(
                    text = freqLabel(freq),
                    style = NeonType.MonoSmall,
                    color = palette.textMuted,
                )
            }
        }
    }
}

private fun freqLabel(hz: Int): String {
    if (hz < 1000) return hz.toString()
    val k = hz / 1000f
    return if (k == k.toInt().toFloat()) "${k.toInt()}k" else "%.1fk".format(k)
}
