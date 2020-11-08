package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.audio.AudioInput
import com.amazon.aace.audio.AudioInputProvider
import timber.log.Timber

class AudioInputProviderHandler(private var context: Context) : AudioInputProvider() {

    private var mDefaultAudioInput: AudioInput? = null

    override fun openChannel(name: String?, type: AudioInputType): AudioInput? {
        Timber.d("openChannel() for type %s", type)
        return if (type == AudioInputType.VOICE || type == AudioInputType.COMMUNICATION) {
            getDefaultAudioInput()
        } else {
            null
        }
    }

    private fun getDefaultAudioInput(): AudioInput? {
        if (mDefaultAudioInput == null) {
            mDefaultAudioInput = AudioInputHandler(context)
        }
        return mDefaultAudioInput
    }
}