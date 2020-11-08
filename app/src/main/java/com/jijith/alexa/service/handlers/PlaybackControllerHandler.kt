package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.alexa.PlaybackController
import com.amazon.aace.audio.AudioOutput
import timber.log.Timber
import java.util.*

class PlaybackControllerHandler(private var context: Context) : PlaybackController() {

    private var mStringBuilder: StringBuilder? = null
    private var mFormatter: Formatter? = null

    private val mCurrentProvider = ""
    private val mCurrentDuration = AudioOutput.TIME_UNKNOWN

    init {
        mStringBuilder = StringBuilder()
        mFormatter = Formatter(mStringBuilder, Locale.US)
    }

    fun previousButtonPressed() {
        Timber.d("Calling PREVIOUS buttonPressed")
        buttonPressed(PlaybackButton.PREVIOUS)
    }

    fun playButtonPressed() {
        Timber.d("Calling PLAY buttonPressed")
        buttonPressed(PlaybackButton.PLAY)
    }

    fun pauseButtonPressed() {
        Timber.d("Calling PAUSE buttonPressed")
        buttonPressed(PlaybackButton.PAUSE)
    }

    fun nextButtonPressed() {
        Timber.d("Calling NEXT buttonPressed")
        buttonPressed(PlaybackButton.NEXT)
    }

    fun skipForwardButtonPressed() {
        Timber.d("Calling SKIP_FORWARD buttonPressed")
        buttonPressed(PlaybackButton.SKIP_FORWARD)
    }

    fun skipBackwardButtonPressed() {
        Timber.d("Calling SKIP_BACKWARD buttonPressed")
        buttonPressed(PlaybackButton.SKIP_BACKWARD)
    }

    fun shuffleTogglePressed(action: Boolean) {
        Timber.v(
            String.format(
                "Calling SHUFFLE togglePressed %s",
                if (action) "selected" else "deselected"
            )
        )
        togglePressed(PlaybackToggle.SHUFFLE, action)
    }

    fun loopTogglePressed(action: Boolean) {
        Timber.v(
            String.format(
                "Calling LOOP togglePressed %s",
                if (action) "selected" else "deselected"
            )
        )
        togglePressed(PlaybackToggle.LOOP, action)
    }

    fun repeatTogglePressed(action: Boolean) {
        Timber.v(
            String.format(
                "Calling REPEAT togglePressed %s",
                if (action) "selected" else "deselected"
            )
        )
        togglePressed(PlaybackToggle.REPEAT, action)
    }

    fun thumbsUpTogglePressed(action: Boolean) {
        Timber.v(
            String.format(
                "Calling THUMBS_UP togglePressed %s",
                if (action) "selected" else "deselected"
            )
        )
        togglePressed(PlaybackToggle.THUMBS_UP, action)
    }

    fun thumbsDownTogglePressed(action: Boolean) {
        Timber.v(
            String.format(
                "Calling THUMBS_DOWN togglePressed %s",
                if (action) "selected" else "deselected"
            )
        )
        togglePressed(PlaybackToggle.THUMBS_DOWN, action)
    }
}