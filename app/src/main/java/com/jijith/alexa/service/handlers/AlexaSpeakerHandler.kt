package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.alexa.AlexaSpeaker
import timber.log.Timber

class AlexaSpeakerHandler(private var context: Context) : AlexaSpeaker() {

    private var mIsMuted = false
    private var mAlexaVolume: Byte = 50
    private var mAlertsVolume: Byte = 50

    override fun speakerSettingsChanged(
        type: SpeakerType,
        local: Boolean,
        volume: Byte,
        mute: Boolean
    ) {
        Timber.i(
            String.format(
                "speakerSettingsChanged [type=%s,local=%b,volume=%d,mute=%b]",
                type.toString(),
                local,
                volume,
                mute
            )
        )
        if (type == SpeakerType.ALEXA_VOLUME) {
            mAlexaVolume = volume
            mIsMuted = mute
        } else if (type == SpeakerType.ALERTS_VOLUME) {
            mAlertsVolume = volume
        }
    }
}