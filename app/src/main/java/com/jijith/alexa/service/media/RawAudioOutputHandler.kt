package com.jijith.alexa.service.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.amazon.aace.audio.AudioOutput
import com.amazon.aace.audio.AudioStream
import timber.log.Timber

class RawAudioOutputHandler(private var context: Context, private var name: String) :
    AudioOutput() {

    private var audioTrack: AudioTrack? = null
    private var audioPlaybackThread: Thread? = null
    private var volume = 0.5f
    private var mutedState = MutedState.UNMUTED
    private var audioStream: AudioStream? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        val audioBufferSize = AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSize,
            AudioTrack.MODE_STREAM
        )
        if (audioTrack!!.state == AudioTrack.STATE_UNINITIALIZED) {
            throw RuntimeException("Failed to create AudioTrack")
        }
    }

    private fun resetPlayer() {
        audioTrack!!.flush()
    }

    fun isPlaying(): Boolean {
        return audioTrack!!.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    //
    // Handle playback directives from Engine
    //

    //
    // Handle playback directives from Engine
    //
    override fun prepare(
        stream: AudioStream?,
        repeating: Boolean
    ): Boolean {
        Timber.v(
            String.format("(%s) Handling prepare()", this.name)
        )
        audioStream = stream
        resetPlayer()
        return true
    }

    override fun prepare(url: String, repeating: Boolean): Boolean {
        throw RuntimeException("URL based playback not supported $url")
    }

    override fun play(): Boolean {
        Timber.v(
            String.format("(%s) Handling play()", this.name)
        )
        audioTrack!!.play()
        audioPlaybackThread = Thread(AudioSampleReadWriteRunnable())
        audioPlaybackThread!!.start()
        return true
    }

    override fun stop(): Boolean {
        Timber.v(
            String.format("(%s) Handling stop()", this.name)
        )
        audioTrack!!.stop()
        return true
    }

    override fun pause(): Boolean {
        Timber.v(
            String.format("(%s) Handling pause()", this.name)
        )
        audioTrack!!.pause()
        return true
    }

    override fun resume(): Boolean {
        Timber.v(
            String.format("(%s) Handling resume()", this.name)
        )
        audioTrack!!.play()
        return true
    }

    override fun setPosition(position: Long): Boolean {
        Timber.v(
            String.format("(%s) Seek is not supported for Raw Audio")
        )
        return true
    }

    override fun getPosition(): Long {
        return Math.abs(audioTrack!!.playbackHeadPosition).toLong()
    }

    //
    // Handle state changes and notify Engine
    //

    //
    // Handle state changes and notify Engine
    //
    private fun onPlaybackStarted() {
        Timber.v(
            String.format("(%s) Media State Changed. STATE: PLAYING",
                this.name
            )
        )
        mediaStateChanged(MediaState.PLAYING)
    }

    private fun onPlaybackStopped() {
        Timber.v(
            String.format("(%s) Media State Changed. STATE: STOPPED",
                this.name
            )
        )
        mediaStateChanged(MediaState.STOPPED)
    }

    private inner class AudioSampleReadWriteRunnable : Runnable {
        override fun run() {
            onPlaybackStarted()
            try {
                Timber.v(
                    String.format("(%s) Audio Playback loop started",
                        this@RawAudioOutputHandler.name
                    )
                )
                val audioBuffer = ByteArray(640)
                while (isPlaying() && !audioStream?.isClosed!!) {
                    val dataRead: Int = audioStream?.read(audioBuffer)!!
                    if (dataRead > 0) {
                        audioTrack?.write(audioBuffer, 0, dataRead)
                    }
                }
            } catch (exp: Exception) {
                Timber.e(exp.message)
                val message = if (exp.message != null) exp.message else ""
                mediaError(MediaError.MEDIA_ERROR_UNKNOWN, message)
            } finally {
                onPlaybackStopped()
            }
            Timber.v(
                String.format("(%s) Audio Playback loop exited", this@RawAudioOutputHandler.name)
            )
        }
    }

    override fun volumeChanged(volume: Float): Boolean {
        if (this.volume == volume) return true
        Timber.i(
            String.format("(%s) Handling setVolume(%s)", this.name, volume)
        )
        this.volume = volume
        if (mutedState == MutedState.MUTED) {
            audioTrack!!.setVolume(0f)
        } else {
            audioTrack!!.setVolume(this.volume)
        }
        return true
    }

    override fun mutedStateChanged(state: MutedState): Boolean {
        if (state != mutedState) {
            Timber.i(
                String.format("Muted state changed (%s) to %s.",
                    this.name, state)
            )
            audioTrack!!.setVolume(if (state == MutedState.MUTED) 0F else volume)
            mutedState = state
        }
        return true
    }

}