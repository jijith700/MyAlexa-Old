package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.audio.AudioOutput
import com.amazon.aace.audio.AudioOutputProvider
import com.jijith.alexa.service.media.RawAudioOutputHandler
import timber.log.Timber
import java.util.*

class AudioOutputProviderHandler(
    private var context: Context,
    private var alexaClientHandler: AlexaClientHandler
) : AudioOutputProvider() {

    private var mAudioOutputMap: HashMap<String, AudioOutput?>? = null

    init {
        mAudioOutputMap = HashMap()
    }

    fun getOutputChannel(name: String): AudioOutput? {
        return if (mAudioOutputMap!!.containsKey(name)) mAudioOutputMap!![name] else null
    }

    override fun openChannel(name: String, type: AudioOutputType): AudioOutput? {
        Timber.d(String.format("openChannel[name=%s,type=%s]", name, type.toString()))
        var audioOutputChannel: AudioOutput? = null
        when (type) {
            AudioOutputType.COMMUNICATION -> audioOutputChannel =
                RawAudioOutputHandler(context, name)
            else -> {
                val audioOutputHandler =
                    AudioOutputHandler(context, name)
                audioOutputChannel = audioOutputHandler
//                alexaClientHandler.registerAuthStateObserver(audioOutputHandler)
            }
        }
        mAudioOutputMap!![name] = audioOutputChannel
        return audioOutputChannel
    }

}