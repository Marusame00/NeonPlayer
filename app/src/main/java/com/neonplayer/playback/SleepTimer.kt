package com.neonplayer.playback

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Immutable
data class SleepTimerState(
    val endsAtMs: Long? = null,
    val endOfTrack: Boolean = false,
)

class SleepTimer(
    private val connection: PlayerConnection,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null

    fun start(minutes: Int) {
        cancel()
        if (minutes <= 0) return
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _state.value = SleepTimerState(endsAtMs = endsAt, endOfTrack = false)
        job = scope.launch {
            delay(minutes * 60_000L)
            connection.pause()
            _state.value = SleepTimerState()
        }
    }

    fun startEndOfTrack() {
        cancel()
        _state.value = SleepTimerState(endsAtMs = null, endOfTrack = true)
        job = scope.launch {
            connection.state.map { it.currentIndex }.distinctUntilChanged().drop(1).first()
            connection.pause()
            _state.value = SleepTimerState()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = SleepTimerState()
    }
}
