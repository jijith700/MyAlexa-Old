package com.jijith.alexa.service.handlers

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.amazon.aace.audio.AudioInput
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

class AudioInputHandler(private var context: Context) : AudioInput() {


    // All audio input consumers expect PCM 16 data @ 16 Khz. We divide this consumption into 10 ms
    // chunks. It comes out at 160 samples every 10 ms to reach 16000 samples (in a second).
    private val samplesToCollectInOneCycle = 160
    private val bytesInEachSample = 2 // PCM 16 = 2 bytes per sample
    private val sampleRateInHz = 16000 //16 khz
    private val audioFramesInBuffer = 5 // Create large enough buffer for 5 audio frames.

    private val executor =
        Executors.newFixedThreadPool(1)
    private var audioInput: AudioRecord? = null
    private var readerRunnable: AudioReaderRunnable? = null


    init {
        audioInput = createAudioInput()
    }


    private fun createAudioInput(): AudioRecord? {
        var audioRecord: AudioRecord? = null
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize: Int =
                minBufferSize + audioFramesInBuffer * samplesToCollectInOneCycle * bytesInEachSample
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: IllegalArgumentException) {
            Timber.e("Cannot create audio input. Error: %s", e.message)
        }
        return audioRecord
    }

    override fun startAudioInput(): Boolean {
        if (audioInput == null) {
            Timber.e("Cannot start audio input. AudioRecord could not be created")
            return false
        }
        if (audioInput!!.state != AudioRecord.STATE_INITIALIZED) {
            // Retry AudioRecord initialization.
            audioInput = createAudioInput()
            if (audioInput!!.state != AudioRecord.STATE_INITIALIZED) {
                Timber.d("Cannot initialize AudioRecord")
                return false
            }
        }
        return startRecording()
    }

    override fun stopAudioInput(): Boolean {
        if (audioInput == null) {
            Timber.w("stopAudioInput() called but AudioRecord was never initialized")
            return false
        }

        // Cancel the audio reader and stop recording
        if (readerRunnable != null) readerRunnable?.cancel()
        try {
            audioInput!!.stop()
        } catch (e: IllegalStateException) {
            Timber.d("AudioRecord cannot stop recording. Error: %s", e.message)
            return false
        }
        return true
    }

    private fun startRecording(): Boolean {
        return if (readerRunnable != null && readerRunnable?.isRunning!!) {
            Timber.d(
                "startRecording() called but AudioRecorder thread is already running"
            )
            false
        } else {
            // Start audio recording
            try {
                audioInput!!.startRecording()
            } catch (e: IllegalStateException) {
                Timber.e("AudioRecord cannot start recording. Error: %s", e.message)
                return false
            }

            // Read recorded audio samples and pass to engine
            try {
                executor.submit(AudioReaderRunnable().also {
                    readerRunnable = it
                }) // Submit the audio reader thread
            } catch (e: RejectedExecutionException) {
                Timber.e(
                    "Audio reader task cannot be scheduled for execution. Error: %s",
                    e.message
                )
                return false
            }
            true
        }
    }

    //
    // AudioReader class
    //

    //
    // AudioReader class
    //
    private inner class AudioReaderRunnable : Runnable {
        var isRunning = true
            private set
        private val mBuffer =
            ByteArray(samplesToCollectInOneCycle * bytesInEachSample)

        fun cancel() {
            isRunning = false
        }

        override fun run() {
            var size: Int
            while (isRunning) {
                size = audioInput?.read(mBuffer, 0, mBuffer.size)!!
                if (size > 0 && isRunning) {
                    write(mBuffer, size.toLong())
                }
            }
        }
    }
}