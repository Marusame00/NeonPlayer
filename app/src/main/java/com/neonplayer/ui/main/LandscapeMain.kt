package com.neonplayer.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonTextButton
import com.neonplayer.ui.library.LibraryPanel
import com.neonplayer.ui.nowplaying.NowPlayingPanel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun LandscapeMain(
    vm: MainViewModel,
    audioPermissionGranted: Boolean,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryPanel(
            vm = vm,
            audioPermissionGranted = audioPermissionGranted,
            onGrantAccess = onGrantAccess,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
        )
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
        ) {
            NeonTopBar(vm = vm, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            NowPlayingPanel(
                vm = vm,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
fun NeonTopBar(
    vm: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "NEON",
            style = NeonType.Label.copy(fontSize = 15.sp, letterSpacing = 5.sp),
            color = palette.textPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "PLAYER",
            style = NeonType.Label.copy(fontSize = 15.sp, letterSpacing = 5.sp),
            color = palette.primary,
        )
        Spacer(Modifier.weight(1f))
        NeonTextButton(
            text = stringResource(R.string.main_stage),
            icon = Icons.Rounded.Fullscreen,
            primary = true,
            onClick = vm::enterStage,
        )
        Spacer(Modifier.width(8.dp))
        NeonIconButton(
            icon = Icons.Rounded.GraphicEq,
            contentDescription = stringResource(R.string.main_equalizer),
            onClick = { vm.openOverlay(Overlay.EQUALIZER) },
            size = 34.dp,
        )
        Spacer(Modifier.width(6.dp))
        NeonIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.main_settings),
            onClick = { vm.openOverlay(Overlay.SETTINGS) },
            size = 34.dp,
        )
    }
}
