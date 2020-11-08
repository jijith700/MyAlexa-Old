package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.phonecontrol.PhoneCallController
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors

class PhoneCallControllerHandler(private var context: Context) : PhoneCallController() {

    private val sDialingToRingingDelay: Long = 2

    private var mCallId: String? = null
    private var mCurrentCallNumber: String? = null
    private var mLastCalledNumber: String? = null

    private val mCallActivated = false
    private var mLocalCallStarted = false
    private val mRemoteCallStarted = false
    private var mCallState: CallState? = null
    private var mConnectionState: ConnectionState? = null
    private var mDeviceConfiguration: HashMap<CallingDeviceConfigurationProperty, Boolean>? =
        null

    private val mExecutor =
        Executors.newSingleThreadExecutor()
    private val mTimer = Timer()

    init {
        mCallId = ""
        mCallState = CallState.IDLE
        mConnectionState = ConnectionState.DISCONNECTED
        mDeviceConfiguration = HashMap()
        mDeviceConfiguration!![CallingDeviceConfigurationProperty.DTMF_SUPPORTED] = false
//        connectionStateChanged(mConnectionState)
    }

    override fun dial(payload: String?): Boolean {
        // Handling should not block the caller
        /*var callId = ""
        var calleeNumber = ""
        try {
            val obj = JSONObject(payload)
            callId = obj.getString("callId")
            calleeNumber = getCalleeDefaultAddressValue(obj)
        } catch (e: JSONException) {
            Timber.e("Error parsing dial directive payload: %s", e.message)
            return false
        }
        mCallId = callId
        mCurrentCallNumber = calleeNumber
        mCallState = CallState.DIALING
        logCallInfo("dial()")
        callStateChanged(CallState.DIALING, callId)
        startDialingCallTimer(sDialingToRingingDelay)
        mLocalCallStarted = true
        updateGUI()*/
        return true
    }

    override fun redial(payload: String?): Boolean {
        Timber.d(payload)
        // Handling should not block the caller
        /* var callId = ""
         callId = try {
             val obj = JSONObject(payload)
             mLogger!!.postJSONTemplate(PhoneCallControllerHandler.sTag, obj.toString(4))
             obj.getString("callId")
         } catch (e: JSONException) {
             Timber.e("Error parsing redial directive payload: "
                         + e.message
             )
             return false
         }
         if (mLastCalledNumber == null || mLastCalledNumber == "") {
             callFailed(callId, CallError.NO_NUMBER_FOR_REDIAL)
             return true
         }
         mCallId = callId
         mCurrentCallNumber = mLastCalledNumber
         mCallState = CallState.DIALING
         logCallInfo("redial()")
         callStateChanged(CallState.DIALING, callId)
         startDialingCallTimer(sDialingToRingingDelay)
         mLocalCallStarted = true
         updateGUI()*/
        return true
    }

    override fun answer(payload: String?) {
        // Handling should not block the caller
        Timber.d(payload)
    }

    override fun stop(payload: String?) {
        // Handling should not block the caller
        Timber.d(payload)
    }

    override fun sendDTMF(payload: String?) {
        // Handling should not block the caller
        Timber.d(payload)
    }

}