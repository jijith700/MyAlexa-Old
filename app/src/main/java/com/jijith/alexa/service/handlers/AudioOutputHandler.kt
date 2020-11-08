package com.jijith.alexa.service.handlers

import android.content.Context
import android.net.Uri
import com.amazon.aace.alexa.AlexaClient
import com.amazon.aace.audio.AudioOutput
import com.amazon.aace.audio.AudioStream
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.jijith.alexa.service.interfaces.AuthStateObserver
import com.jijith.alexa.service.media.MediaSourceFactory
import timber.log.Timber
import java.io.IOException

class AudioOutputHandler(private var context: Context, private var name: String) : AudioOutput(),
    AuthStateObserver {

    private val FILENAME = "alexa_media" // Note: not thread safe

    private val SKIP_THRESHOLD_IN_MS: Long = 1500 // 1500 ms

    private var mediaSourceFactory: MediaSourceFactory? = null
    private var player: SimpleExoPlayer? = null
    private var repeating = false

    private var volume = 0.5f
    private var mutedState = MutedState.UNMUTED

    private var period: Timeline.Period? = null
    private var window: Timeline.Window? = null
    private var position: Long = 0
    private var lastLivePosition: Long = 0

    private var livePausedPosition: Long = 0
    private var savedPeriodIndex = 0
    private var savedWindowIndex = 0
    private var livePausedOffset: Long = 0
    private var liveResumedOffset: Long = 0
    private var newPlayReceieved = false

    init {
        mediaSourceFactory = MediaSourceFactory(context, name)
        repeating = false
        period = Timeline.Period()
        window = Timeline.Window()
        initializePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
        player?.addListener(PlayerEventListener())
        player?.setPlayWhenReady(false)
    }

    private fun resetPlayer() {
        player?.repeatMode = Player.REPEAT_MODE_OFF
        player?.playWhenReady = false
        player?.stop(true)
        // reset live station offsets
        liveResumedOffset = 0
        livePausedPosition = 0
    }

    fun isPlaying(): Boolean {
        return (player != null && player?.playWhenReady!!
                && (player?.getPlaybackState() === Player.STATE_BUFFERING
                || player?.getPlaybackState() === Player.STATE_READY))
    }

    //
    // Handle playback directives from Engine
    //

    //
    // Handle playback directives from Engine
    //
    override fun prepare(
        stream: AudioStream,
        repeating: Boolean
    ): Boolean {
        Timber.d(String.format("(%s) Handling prepare()", name))
        resetPlayer()
        this.repeating = repeating
        try {
            context?.openFileOutput(FILENAME, Context.MODE_PRIVATE).use { os ->
                val buffer = ByteArray(4096)
                var size: Int
                while (!stream.isClosed) {
                    while (stream.read(buffer).also { size = it } > 0) os.write(buffer, 0, size)
                }
            }
        } catch (e: IOException) {
            Timber.e(e)
            return false
        }
        return try {
            val uri =
                Uri.fromFile(context?.getFileStreamPath(FILENAME))
            val mediaSource: MediaSource = mediaSourceFactory?.createFileMediaSource(uri)!!
            player?.prepare(mediaSource, true, false)
            true
        } catch (e: Exception) {
            Timber.e(e.message)
            val message = if (e.message != null) e.message else ""
            mediaError(MediaError.MEDIA_ERROR_UNKNOWN, message)
            false
        }
    }

    override fun prepare(url: String?, repeating: Boolean): Boolean {
        Timber.v(String.format("(%s) Handling prepare(url)", name))
        resetPlayer()
        this.repeating = repeating
        val uri = Uri.parse(url)
        return try {
            val mediaSource: MediaSource = mediaSourceFactory?.createHttpMediaSource(uri)!!
            player?.prepare(mediaSource, true, false)
            true
        } catch (e: Exception) {
            val message = if (e.message != null) e.message else ""
            Timber.e(message)
            mediaError(MediaError.MEDIA_ERROR_UNKNOWN, message)
            false
        }
    }

    override fun play(): Boolean {
        Timber.v(String.format("(%s) Handling play()", name))
        newPlayReceieved = true // remember new play received
        savedPeriodIndex = player?.currentPeriodIndex!! // remember period index
        player?.setPlayWhenReady(true)
        return true
    }

    override fun stop(): Boolean {
        Timber.v(String.format("(%s) Handling stop()", name))
        if (!player?.playWhenReady!!) {
            // Player is already not playing. Notify Engine of stop
            onPlaybackStopped()
        } else player?.setPlayWhenReady(false)
        return true
    }

    override fun pause(): Boolean {
        Timber.v(String.format("(%s) Handling pause()", name))
        val currentTimeline: Timeline = player?.currentTimeline!!
        if (!currentTimeline.isEmpty() && player?.isCurrentWindowDynamic!!) { // If pausing live station.
            livePausedOffset = 0
            livePausedPosition = position // save paused position
        }
        player?.setPlayWhenReady(false)
        return true
    }

    override fun resume(): Boolean {
        Timber.v(String.format("(%s) Handling resume()", name))
        val currentTimeline: Timeline = player?.currentTimeline!!
        if (!currentTimeline.isEmpty() && player?.isCurrentWindowDynamic!!) {  // If resuming live station reset to 0.
            player?.seekToDefaultPosition() // reset player position to its default
            livePausedOffset =
                Math.abs(player?.currentPosition!!) // get the new position
            livePausedOffset -= currentTimeline.getPeriod(savedPeriodIndex, period)
                .getPositionInWindowMs() // adjust for window
            livePausedOffset -= liveResumedOffset // adjust for stopped offset
            livePausedOffset -= livePausedPosition // adjust for paused offset
            livePausedPosition = 0 // reset paused position
        }
        player?.setPlayWhenReady(true)
        return true
    }

    override fun setPosition(position: Long): Boolean {
        Timber.v(
            String.format("(%s) Handling setPosition(%s)", name, position)
        )
        player?.seekTo(position)
        liveResumedOffset -= position
        return true
    }

    override fun getPosition(): Long {
        val currentTimeline: Timeline = player?.currentTimeline!!
        position = Math.abs(player?.currentPosition!!)
        if (!currentTimeline.isEmpty() && player?.isCurrentWindowDynamic!!) {
            if (livePausedPosition == 0L) { // not during pause
                position -= currentTimeline.getPeriod(savedPeriodIndex, period)
                    .getPositionInWindowMs() // Adjust position to be relative to start of period rather than window.
                position -= liveResumedOffset // Offset saved for live station stopped / played
                position -= livePausedOffset // Offset saved for live station paused / resumed
                Timber.v(
                    java.lang.String.format(
                        "DEBUG, mPosition (%s), mSavedPeriodIndex (%s), mPeriod (%s), mLiveResumedOffset (%s),  mLivePausedOffset (%s)",
                        position, savedPeriodIndex, period, liveResumedOffset, livePausedOffset
                    )
                )
            } else {
                Timber.v(
                    String.format(
                        "(%s) Handling livePaused getPosition(%s)",
                        name,
                        livePausedPosition
                    )
                )
                return livePausedPosition // the saved position during a live station paused state
            }
            val skipDifference = position - lastLivePosition
            // difference in position normally 1000ms +/- 10ms
            if (skipDifference > SKIP_THRESHOLD_IN_MS) { // skips 10 - 20s while resuming live radio sometimes, if significant skip: update live resumed offset
                Timber.v(
                    String.format("getPosition live position skipped: (%s)", skipDifference)
                )
                liveResumedOffset += skipDifference
                position -= skipDifference
            }
            lastLivePosition = position
        }
        Timber.v(
            String.format("(%s) Handling getPosition(%s)", name, position)
        )
        return position
    }

    override fun getDuration(): Long {
        val duration: Long = player?.duration!!
        return if (duration != C.TIME_UNSET) duration else TIME_UNKNOWN
    }

    override fun volumeChanged(volume: Float): Boolean {
        if (this.volume != volume) {
            Timber.i(
                String.format("(%s) Handling volumeChanged(%s)", name, volume)
            )
            this.volume = volume
            if (mutedState == MutedState.MUTED) {
                player?.volume = 0F
            } else {
                player?.setVolume(volume)
            }
        }
        return true
    }

    override fun mutedStateChanged(state: MutedState): Boolean {
        if (state != mutedState) {
            Timber.i(
                String.format("Muted state changed (%s) to %s.", name, state)
            )
            player?.setVolume(if (state == MutedState.MUTED) 0F else volume)
            mutedState = state
        }
        return true
    }

    //
    // Handle ExoPlayer state changes and notify Engine
    //

    //
    // Handle ExoPlayer state changes and notify Engine
    //
    private fun onPlaybackStarted() {
        Timber.v(
            String.format("(%s) Media State Changed. STATE: PLAYING", name)
        )
        mediaStateChanged(MediaState.PLAYING)
        savedWindowIndex = player?.currentWindowIndex!! // remember window index
        player?.currentTimeline?.getWindow(savedWindowIndex, window) // set window
        savedPeriodIndex = player?.currentPeriodIndex!! // remember period index
        player?.currentTimeline?.getPeriod(savedWindowIndex, period) // set period
        /*mLogger.postVerbose( String.format( "(%s) Media State Changed. STATE: PLAYING, mSavedWindowIndex (%s), fPI (%s), LPI (%s), winduration(%s)",
                name, mSavedWindowIndex, mWindow.firstPeriodIndex, mWindow.lastPeriodIndex, mWindow.durationUs ) );*/if (newPlayReceieved && player?.isCurrentWindowDynamic!!) { // remember offset if new play for live station
            player?.seekToDefaultPosition()
            liveResumedOffset += Math.abs(player?.currentPosition!!)
            newPlayReceieved = false
        }
    }

    private fun onPlaybackStopped() {
        Timber.v(String.format("(%s) Media State Changed. STATE: STOPPED", name))
        mediaStateChanged(MediaState.STOPPED)
    }

    private fun onPlaybackFinished() {
        if (repeating) {
            player?.seekTo(0)
            player?.setRepeatMode(Player.REPEAT_MODE_ONE)
        } else {
            player?.setRepeatMode(Player.REPEAT_MODE_OFF)
            Timber.v(String.format("(%s) Media State Changed. STATE: STOPPED", name))
            mediaStateChanged(MediaState.STOPPED)
        }
    }

    private fun onPlaybackBuffering() {
        Timber.v(String.format("(%s) Media State Changed. STATE: BUFFERING", name))
        mediaStateChanged(MediaState.BUFFERING)
    }

    override fun onAuthStateChanged(
        state: AlexaClient.AuthState,
        error: AlexaClient.AuthError?
    ) {
        if (state == AlexaClient.AuthState.UNINITIALIZED) {
            // Stop playing media if user logs out
            if (player?.playWhenReady!!) {
                // Ensure media is playing before stopping
                Timber.i(
                    String.format(
                        "(%s) Auth state is uninitialized. Stopping media player",
                        name
                    )
                )
                player?.setPlayWhenReady(false)
            }
        }
    }

    //
    // ExoPlayer event listener
    //
    private inner class PlayerEventListener : Player.DefaultEventListener() {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> if (playWhenReady) onPlaybackFinished()
                Player.STATE_READY -> if (playWhenReady) onPlaybackStarted() else onPlaybackStopped()
                Player.STATE_BUFFERING -> if (playWhenReady) onPlaybackBuffering()
                else -> {
                }
            }
        }

        override fun onPlayerError(e: ExoPlaybackException) {
            val message: String
            message = if (e.type === ExoPlaybackException.TYPE_SOURCE) {
                "ExoPlayer Source Error: " + e.getSourceException().message!!
            } else if (e.type === ExoPlaybackException.TYPE_RENDERER) {
                "ExoPlayer Renderer Error: " + e.getRendererException().message!!
            } else if (e.type === ExoPlaybackException.TYPE_UNEXPECTED) {
                "ExoPlayer Unexpected Error: " + e.getUnexpectedException().message!!
            } else {
                e.message!!
            }
            Timber.e("PLAYER ERROR: $message")
            mediaError(MediaError.MEDIA_ERROR_INTERNAL_DEVICE_ERROR, message)
        }
    }
}