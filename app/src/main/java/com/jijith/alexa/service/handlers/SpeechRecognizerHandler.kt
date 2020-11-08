package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.alexa.SpeechRecognizer
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors

class SpeechRecognizerHandler(private var context: Context, private var wakeWordEnabled: Boolean) :
    SpeechRecognizer(wakeWordEnabled) {

    private var mAudioCueObservable: AudioCueObservable? = AudioCueObservable()
    private val mExecutor = Executors.newFixedThreadPool(1)
    private var mAllowStopCapture = false // Only true if holdToTalk() returned true

    override fun wakewordDetected(wakeWord: String?): Boolean {
        mAudioCueObservable?.playAudioCue(AudioCueState.START_VOICE)

        // Notify Error state to AutoVoiceChrome if disconnected with Alexa
        return true
    }

    override fun endOfSpeechDetected() {
        mAudioCueObservable?.playAudioCue(AudioCueState.END)
    }

    fun onTapToTalk() {
        if (tapToTalk()) mAudioCueObservable?.playAudioCue(AudioCueState.START_TOUCH)
    }

    fun onHoldToTalk() {
        mAllowStopCapture = false
        if (holdToTalk()) {
            mAllowStopCapture = true
            mAudioCueObservable?.playAudioCue(AudioCueState.START_TOUCH)
        }
    }

    fun onReleaseHoldToTalk() {
        if (mAllowStopCapture) stopCapture()
        mAllowStopCapture = false
    }


    fun enableWakeWord() {
        Timber.d("wakeword enabled")
        mExecutor.submit { enableWakewordDetection() }
    }

    fun disableWakeWord() {
        Timber.d("wakeword disabled")
        mExecutor.submit { disableWakewordDetection() }
    }

    /* For playing speech recognition audio cues */

    /* For playing speech recognition audio cues */
    enum class AudioCueState {
        START_TOUCH, START_VOICE, END
    }

    class AudioCueObservable : Observable() {
        fun playAudioCue(state: AudioCueState?) {
            setChanged()
            notifyObservers(state)
        }
    }

    fun addObserver(observer: Observer?) {
        if (mAudioCueObservable == null) mAudioCueObservable = AudioCueObservable()
        mAudioCueObservable!!.addObserver(observer)
    }
}