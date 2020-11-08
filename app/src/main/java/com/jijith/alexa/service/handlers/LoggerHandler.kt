package com.jijith.alexa.service.handlers

import android.graphics.Color
import android.util.Log
import com.amazon.aace.logger.Logger
import com.jijith.alexa.utils.LogEntry
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

class LoggerHandler : Logger() {

    /* Log colors */
    private val sColorVerbose = Color.parseColor("#B3E5FC") // Light Blue

    private val sColorInfo = Color.parseColor("#FFFFFF") // White

    private val sColorMetric = Color.parseColor("#73C54C") // Light Green

    private val sColorWarn = Color.parseColor("#F57F17") // Orange

    private val sColorError = Color.parseColor("#D50000") // Red

    private val sColorJsonTemplate = Color.parseColor("#F9B702") // Gold


    private val sTimeFormat // Note: not thread safe
            =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val sLevel =
        Level.VERBOSE
    private val sClientSourceTag = "CLI"

    private var mObservable: LoggerObservable? = null

    fun LoggerHandler() {
        mObservable = LoggerObservable()
    }

    // Handle log from Auto SDK
    override fun logEvent(
        level: Level,
        time: Long,
        source: String?,
        message: String?
    ): Boolean {
        if (level.ordinal >= sLevel.ordinal) {
            val color: Int
            color = when (level) {
                Level.VERBOSE -> sColorVerbose
                Level.WARN -> sColorWarn
                Level.CRITICAL, Level.ERROR -> sColorError
                Level.INFO -> sColorInfo
                Level.METRIC -> sColorMetric
                else -> sColorInfo
            }

            // Configure log for GUI log view
            val json = JSONObject()
            try {
                json.put(
                    "text", String.format(
                        "%-25s [%s] %3c%n%s",
                        sTimeFormat.format(time), source, level.toChar(), message
                    )
                )
                json.put("textColor", color)
                json.put("source", source) // For log view filtering
                json.put("level", level.toString()) // For log view filtering
            } catch (e: JSONException) {
                Timber.e("%s Error: %s", sClientSourceTag, e.toString())
                return true
            }
            mObservable?.log(json)
        }
        return true
    }

    /* Client level log methods. Will use Auto SDK Logger */

    /* Client level log methods. Will use Auto SDK Logger */
    fun postVerbose(tag: String?, message: String?) {
        log(Level.VERBOSE, tag, message)
    }

    fun postInfo(tag: String?, message: String?) {
        log(Level.INFO, tag, message)
    }

    fun postWarn(tag: String?, message: String?) {
        log(Level.WARN, tag, message)
    }

    fun postError(tag: String?, message: String?) {
        log(Level.ERROR, tag, message)
    }

    fun postError(tag: String?, thr: Throwable) {
        try {
            ByteArrayOutputStream().use { os ->
                val ps = PrintStream(os)
                thr.printStackTrace(ps)
                val str = os.toString()
                log(Level.ERROR, tag, str)
            }
        } catch (e: IOException) {
            Timber.e("%s Error: %s", sClientSourceTag, e.toString())
        }
    }

    /* Additional client logs. Will insert log into GUI log view but not use Auto SDK Logger */

    /* Additional client logs. Will insert log into GUI log view but not use Auto SDK Logger */ // Client log for JSON Templates
    fun postJSONTemplate(tag: String?, message: String?) {
        val currentTime = Calendar.getInstance().time
        val level = Level.INFO
        val json = JSONObject()
        try {
            json.put(
                "text", String.format(
                    "%-25s [%s] %3c%n%s:%n%s",
                    sTimeFormat.format(currentTime), sClientSourceTag,
                    level.toChar(), tag, message
                )
            )
            json.put("textColor", sColorJsonTemplate)
            json.put("source", sClientSourceTag) // For log view filtering
            json.put("level", level.toString()) // For log view filtering
        } catch (e: JSONException) {
            Timber.e("%s Error: %s", sClientSourceTag, e.toString())
            return
        }
//        mObservable?.log(json, LogRecyclerViewAdapter.JSON_TEXT)
    }

    // Client log for display cards
    fun postDisplayCard(template: JSONObject?, logType: Int) {
        val level = Level.INFO
        val json = JSONObject()
        try {
            json.put("template", template)
            json.put("source", sClientSourceTag) // For log view filtering
            json.put("level", level.toString()) // For log view filtering
        } catch (e: JSONException) {
            Timber.e("%s Error: %s", sClientSourceTag, e.toString())
        }
        mObservable?.log(json, logType)
    }

    /* Logger Observable for inserting logs into GUI log view */

    /* Logger Observable for inserting logs into GUI log view */
    class LoggerObservable : Observable() {
        fun log(message: String?) {
            setChanged()
            notifyObservers(message)
        }

        fun log(obj: JSONObject?) {
            setChanged()
//            notifyObservers(LogEntry(LogRecyclerViewAdapter.TEXT_LOG, obj))
        }

        fun log(obj: JSONObject?, logType: Int) {
            setChanged()
            notifyObservers(LogEntry(logType, obj))
        }
    }

    fun addLogObserver(observer: Observer?) {
        mObservable?.addObserver(observer)
    }

}