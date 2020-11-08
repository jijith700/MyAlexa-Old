package com.jijith.alexa.service.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.annotation.Nullable
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import timber.log.Timber
import java.io.IOException

class MediaSourceFactory(private var context: Context, private var name: String) {

    private val sUserAgentName = "com.jijith.alexa"

    private val mainHandler = Handler()
    private val playlistParser: PlaylistParser = PlaylistParser()
    private val mediaSourceListener = MediaSourceListener()
    private val fileDataSourceFactory: DataSource.Factory =
        FileDataSourceFactory(null)
    private var httpDataSourceFactory: DataSource.Factory? =
        null

    init {
        httpDataSourceFactory = buildHttpDataSourceFactory(context)
    }

    private fun buildHttpDataSourceFactory(context: Context?): HttpDataSource.Factory? {
        val userAgent =
            Util.getUserAgent(context, sUserAgentName)
        // Some streams may see a long response time to begin data transfer from server after
        // connection. Use default 8 second connection timeout and increased 20 second read timeout
        // to catch this case and avoid reattempts to connect that will continue to time out.
        // May perceive long "dead time" in cases where data read takes a long time
        return DefaultHttpDataSourceFactory(
            userAgent, null, 8000,
            20000, true
        )
    }

    @Throws(Exception::class)
    fun createFileMediaSource(uri: Uri): MediaSource? {
        return createMediaSource(
            uri, fileDataSourceFactory, mediaSourceListener, mainHandler,
            playlistParser
        )
    }

    @Throws(Exception::class)
    fun createHttpMediaSource(uri: Uri): MediaSource? {
        return createMediaSource(
            uri, httpDataSourceFactory, mediaSourceListener, mainHandler,
            playlistParser
        )
    }

    @Throws(Exception::class)
    private fun createMediaSource(
        uri: Uri,
        dataSourceFactory: DataSource.Factory?,
        mediaSourceListener: MediaSourceEventListener,
        handler: Handler,
        playlistParser: PlaylistParser
    ): MediaSource? {
        val type = MediaType.inferContentType(uri.lastPathSegment)
        return when (type) {
            MediaType.DASH -> DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(dataSourceFactory),
                dataSourceFactory
            ).createMediaSource(uri, handler, mediaSourceListener)
            MediaType.SMOOTH_STREAMING -> SsMediaSource.Factory(
                DefaultSsChunkSource.Factory(dataSourceFactory),
                dataSourceFactory
            ).createMediaSource(uri, handler, mediaSourceListener)
            MediaType.HLS -> HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri, handler, mediaSourceListener)
            MediaType.M3U, MediaType.PLS -> {
                val parsedUri: Uri = playlistParser.parseUri(uri)!!
                createMediaSource(
                    parsedUri, dataSourceFactory, mediaSourceListener,
                    handler, playlistParser
                )
            }
            MediaType.OTHER -> ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri, handler, mediaSourceListener)
            else -> throw IllegalStateException("Unsupported type")
        }
    }

    //
    // Media types for creating an ExoPlayer MediaSource
    //
    internal enum class MediaType(val type: Int) {
        DASH(C.TYPE_DASH), SMOOTH_STREAMING(C.TYPE_SS), HLS(C.TYPE_HLS), OTHER(C.TYPE_OTHER), M3U(4), PLS(
            5
        );

        companion object {
            fun inferContentType(@Nullable fileExtension: String?): MediaType {
                return if (fileExtension == null) {
                    OTHER
                } else if (fileExtension.endsWith(".ashx") || fileExtension.endsWith(".m3u")) {
                    M3U
                } else if (fileExtension.endsWith(".pls")) {
                    PLS
                } else {
                    val type =
                        Util.inferContentType(fileExtension)
                    for (mediaType in values()) {
                        if (mediaType.type == type) return mediaType
                    }
                    OTHER
                }
            }
        }

    }

    //
    // Media Source event listener
    //
    private inner class MediaSourceListener : MediaSourceEventListener {
        private var mRetryCount = 0
        override fun onLoadStarted(
            dataSpec: DataSpec?,
            dataType: Int,
            trackType: Int,
            trackFormat: Format?,
            trackSelectionReason: Int,
            trackSelectionData: Any?,
            mediaStartTimeMs: Long,
            mediaEndTimeMs: Long,
            elapsedRealtimeMs: Long
        ) {
            mRetryCount = 1
            Timber.v(String.format("(%s) Load media started", name))
        }

        override fun onDownstreamFormatChanged(
            trackType: Int,
            trackFormat: Format?,
            trackSelectionReason: Int,
            trackSelectionData: Any?,
            mediaTimeMs: Long
        ) {
        }

        override fun onUpstreamDiscarded(
            trackType: Int,
            mediaStartTimeMs: Long,
            mediaEndTimeMs: Long
        ) {
        }

        override fun onLoadCompleted(
            dataSpec: DataSpec?,
            dataType: Int,
            trackType: Int,
            trackFormat: Format?,
            trackSelectionReason: Int,
            trackSelectionData: Any?,
            mediaStartTimeMs: Long,
            mediaEndTimeMs: Long,
            elapsedRealtimeMs: Long,
            loadDurationMs: Long,
            bytesLoaded: Long
        ) {
            mRetryCount = 0
        }

        override fun onLoadCanceled(
            dataSpec: DataSpec?,
            dataType: Int,
            trackType: Int,
            trackFormat: Format?,
            trackSelectionReason: Int,
            trackSelectionData: Any?,
            mediaStartTimeMs: Long,
            mediaEndTimeMs: Long,
            elapsedRealtimeMs: Long,
            loadDurationMs: Long,
            bytesLoaded: Long
        ) {
            Timber.v(String.format("(%s) Load media cancelled", name))
            mRetryCount = 0
        }

        override fun onLoadError(
            dataSpec: DataSpec?,
            dataType: Int,
            trackType: Int,
            trackFormat: Format?,
            trackSelectionReason: Int,
            trackSelectionData: Any?,
            mediaStartTimeMs: Long,
            mediaEndTimeMs: Long,
            elapsedRealtimeMs: Long,
            loadDurationMs: Long,
            bytesLoaded: Long,
            error: IOException?,
            wasCanceled: Boolean
        ) {
            Timber.v(String.format("(%s) Error loading media. Attempts: %s", name, mRetryCount))
            mRetryCount++
        }
    }

}