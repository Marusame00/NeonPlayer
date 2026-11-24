package com.neonplayer.ui.backdrop

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.neonplayer.R
import com.neonplayer.domain.model.AppSettings
import com.neonplayer.ui.components.gridTexture
import com.neonplayer.ui.theme.Neon
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Backdrop(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    forceStatic: Boolean = false,
) {
    val context = LocalContext.current
    val palette = Neon.palette
    val option = remember(settings.backdropId, settings.customBackdrops) {
        BackdropCatalog.resolve(context, settings.backdropId, settings.customBackdrops)
    }
    Box(modifier.fillMaxSize()) {
        val playUri = option.playUri
        if (forceStatic || settings.batterySaver || option.kind == BackdropKind.STATIC || playUri == null) {
            StaticBackdrop(Modifier.fillMaxSize())
        } else {
            VideoBackdrop(playUri, Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = settings.backdropDim.coerceIn(0f, 0.8f))),
        )
        if (settings.backdropVignette) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val vignette = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.72f,
                        )
                        onDrawBehind { drawRect(brush = vignette) }
                    },
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .gridTexture(palette.primary.copy(alpha = 0.035f)),
        )
    }
}

@Composable
fun StaticBackdrop(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.backdrop_synthwave),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun VideoBackdrop(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var rendered by remember(uri) { mutableStateOf(false) }
    val fade by animateFloatAsState(
        targetValue = if (rendered) 1f else 0f,
        animationSpec = tween(600),
        label = "videoFade",
    )
    val viewHolder = remember(uri) { mutableStateOf<PlayerView?>(null) }
    val exo = remember(uri) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()
            .apply {
                volume = 0f
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .setMaxVideoSize(1920, 1080)
                    .build()
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
    }
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                rendered = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exo.seekTo(0)
                    exo.play()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                rendered = false
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            viewHolder.value?.player = null
            viewHolder.value = null
            exo.release()
        }
    }
    DisposableEffect(lifecycleOwner, exo) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exo.pause()
                Lifecycle.Event.ON_START -> {
                    if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0)
                    exo.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(modifier.fillMaxSize()) {
        StaticBackdrop(Modifier.fillMaxSize())
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                    player = exo
                    viewHolder.value = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade },
        )
    }
}

@Composable
fun BackdropThumbnail(option: BackdropOption, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val assetFile = option.assetFile
    if (option.kind == BackdropKind.STATIC || option.thumbUri == null) {
        StaticBackdrop(modifier)
        return
    }
    if (assetFile != null) {
        val poster by produceState<String?>(initialValue = null, assetFile) {
            value = withContext(Dispatchers.IO) { BackdropCatalog.posterFor(context, assetFile) }
        }
        val path = poster
        if (path != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(path))
                    .size(320)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        } else {
            StaticBackdrop(modifier)
        }
        return
    }
    var failed by remember(option.thumbUri) { mutableStateOf(false) }
    Box(modifier) {
        if (!failed) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(option.thumbUri)
                    .videoFrameMillis(1000)
                    .size(320)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) failed = true
                },
            )
        }
        if (failed) StaticBackdrop(Modifier.fillMaxSize())
    }
}
