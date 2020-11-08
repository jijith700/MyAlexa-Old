package com.jijith.alexa.service.handlers

import android.os.Handler
import android.os.Message
import com.amazon.aace.alexa.AudioPlayer
import com.amazon.aace.audio.AudioOutput
import timber.log.Timber

class AudioPlayerHandler(private var audioOutputProvider: AudioOutputProviderHandler,
                         private var playbackController: PlaybackControllerHandler) : AudioPlayer() {

    private var mAudioPlayerStateHandler: AudioPlayerStateHandler? = null

    init {
        mAudioPlayerStateHandler = AudioPlayerStateHandler()
    }

    override fun playerActivityChanged(state: PlayerActivity) {
        Timber.i(String.format("playerActivityChanged: %s", state.toString()))
        mAudioPlayerStateHandler!!.sendEmptyMessage(state.ordinal)
    }

    //
    // ProgressHandler
    //
    private val UPDATE_PROGRESS = Int.MAX_VALUE

    private inner class AudioPlayerStateHandler internal constructor() : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == UPDATE_PROGRESS) {
                val audioOutput: AudioOutput? = audioOutputProvider.getOutputChannel("AudioPlayer")
                if (audioOutput != null) {
                    var position = audioOutput.position
                    if (audioOutput.duration == AudioOutput.TIME_UNKNOWN) {
                        position = AudioOutput.TIME_UNKNOWN
                    }
//                    playbackController.setTime(position, audioOutput.duration)
                    sendEmptyMessageDelayed(UPDATE_PROGRESS, 1000 - position % 1000)
                }
            } else if (msg.what == PlayerActivity.PLAYING.ordinal) {
//                playbackController.start()
                sendEmptyMessage(UPDATE_PROGRESS)
            } else if (msg.what == PlayerActivity.STOPPED.ordinal) {
//                playbackController.stop()
                removeMessages(UPDATE_PROGRESS)
            } else if (msg.what == PlayerActivity.FINISHED.ordinal) {
//                playbackController.reset()
                removeMessages(UPDATE_PROGRESS)
            }
        }
    }
}