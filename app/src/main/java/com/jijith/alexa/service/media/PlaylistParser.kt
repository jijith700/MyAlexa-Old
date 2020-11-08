package com.jijith.alexa.service.media

import android.net.Uri
import androidx.annotation.Nullable
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.regex.Pattern

class PlaylistParser {

    private val TAG = PlaylistParser::class.java.simpleName
    private val RESPONSE_OK = 200
    private val PATTERN = Pattern.compile("https?:.*")
    private val executor =
        Executors.newSingleThreadExecutor()

    // Extracts Url from redirect Url. Note: not a complete playlist parser implementation
    @Throws(IOException::class)
    fun parseUri(uri: Uri): Uri? {
        return parsePlaylist(parseResponse(getResponse(uri)))
    }

    @Throws(IOException::class)
    private fun getResponse(uri: Uri): InputStream? {
        val response =
            executor.submit(Callable {
                var con: HttpURLConnection? = null
                try {
                    val obj = URL(uri.toString())
                    con = obj.openConnection() as HttpURLConnection
                    val responseCode = con.responseCode
                    return@Callable if (responseCode == RESPONSE_OK) {
                        con?.inputStream
                    } else {
                        throw IOException("$TAG: Unsuccessful response. Code: $responseCode")
                    }
                } finally {
                    con?.disconnect()
                }
            })
        return try {
            response.get()
        } catch (e: Exception) {
            throw IOException(TAG + ": Error getting response: " + e.message)
        }
    }

    @Throws(IOException::class)
    private fun parseResponse(inStream: InputStream?): String? {
        if (inStream != null) {
            val `in` =
                BufferedReader(InputStreamReader(inStream))
            var inputLine: String?
            val response = StringBuilder()
            return try {
                while (`in`.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                response.toString()
            } catch (e: Exception) {
                throw IOException("$TAG: Error parsing response")
            } finally {
                inStream.close()
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun parsePlaylist(@Nullable playlist: String?): Uri? {
        return if (playlist != null && !playlist.isEmpty()) {
            val matcher = PATTERN.matcher(playlist)
            if (matcher.find()) {
                Uri.parse(playlist.substring(matcher.start(), matcher.end()))
            } else {
                throw IOException("$TAG: Response did not contain a URL")
            }
        } else {
            throw IOException("$TAG: Response was empty")
        }
    }

}