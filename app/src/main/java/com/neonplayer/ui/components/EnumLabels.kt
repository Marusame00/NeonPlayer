package com.neonplayer.ui.components

import android.content.Context
import com.neonplayer.R
import com.neonplayer.domain.model.AccentMode
import com.neonplayer.domain.model.LibraryTab
import com.neonplayer.domain.model.OrientationLock
import com.neonplayer.domain.model.ReplayGainMode
import com.neonplayer.domain.model.SortOrder
import com.neonplayer.domain.model.StageLayout
import com.neonplayer.domain.model.VisualizerStyle

fun AccentMode.label(context: Context): String = context.getString(
    when (this) {
        AccentMode.CYAN -> R.string.accent_cyan
        AccentMode.MAGENTA -> R.string.accent_magenta
    }
)

fun VisualizerStyle.label(context: Context): String = context.getString(
    when (this) {
        VisualizerStyle.BARS -> R.string.viz_bars
        VisualizerStyle.MIRRORED -> R.string.viz_mirrored
        VisualizerStyle.RADIAL -> R.string.viz_radial
        VisualizerStyle.OSCILLOSCOPE -> R.string.viz_oscilloscope
        VisualizerStyle.RETRO_GRID -> R.string.viz_retro_grid
    }
)

fun StageLayout.label(context: Context): String = context.getString(
    when (this) {
        StageLayout.ART_LEFT -> R.string.stage_layout_art_left
        StageLayout.CENTERED_RING -> R.string.stage_layout_centered
        StageLayout.TEXT_OVER_VISUALIZER -> R.string.stage_layout_text_over
        StageLayout.VISUALIZER_ONLY -> R.string.stage_layout_viz_only
    }
)

fun OrientationLock.label(context: Context): String = context.getString(
    when (this) {
        OrientationLock.LANDSCAPE -> R.string.orientation_landscape
        OrientationLock.AUTO -> R.string.orientation_auto
        OrientationLock.PORTRAIT -> R.string.orientation_portrait
    }
)

fun ReplayGainMode.label(context: Context): String = context.getString(
    when (this) {
        ReplayGainMode.OFF -> R.string.replaygain_off
        ReplayGainMode.TRACK -> R.string.replaygain_track
        ReplayGainMode.ALBUM -> R.string.replaygain_album
    }
)

fun SortOrder.label(context: Context): String = context.getString(
    when (this) {
        SortOrder.TITLE -> R.string.sort_title
        SortOrder.ARTIST -> R.string.sort_artist
        SortOrder.ALBUM -> R.string.sort_album
        SortOrder.DATE_ADDED -> R.string.sort_date_added
        SortOrder.DURATION -> R.string.sort_duration
        SortOrder.MANUAL -> R.string.sort_manual
    }
)

fun LibraryTab.label(context: Context): String = context.getString(
    when (this) {
        LibraryTab.SONGS -> R.string.tab_songs
        LibraryTab.ALBUMS -> R.string.tab_albums
        LibraryTab.ARTISTS -> R.string.tab_artists
        LibraryTab.PLAYLISTS -> R.string.tab_playlists
        LibraryTab.FOLDERS -> R.string.tab_folders
    }
)
