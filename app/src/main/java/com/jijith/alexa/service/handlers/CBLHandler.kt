package com.jijith.alexa.service.handlers

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.amazon.aace.alexa.AlexaClient
import com.amazon.aace.cbl.CBL
import com.jijith.alexa.R
import com.jijith.alexa.service.interfaces.AuthStateObserver
import com.jijith.alexa.service.interfaces.managers.AlexaEngineManager
import com.jijith.alexa.service.interfaces.managers.DatabaseManager
import com.jijith.alexa.utils.USER_CODE
import com.jijith.alexa.utils.VERIFICATION_URI
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class CBLHandler(
    private var alexaEngineManager: AlexaEngineManager,
    private var context: Context,
    private var databaseManager: DatabaseManager
) : CBL() {

    private var refreshToken = ""

    init {
        refreshToken = databaseManager.getRefreshToken()
    }

    override fun cblStateChanged(
        state: CBLState?,
        reason: CBLStateChangedReason?,
        url: String?,
        code: String?
    ) {
        super.cblStateChanged(state, reason, url, code)
        Timber.d(
            "cblStateChanged:state=%s,reason=%s,url=%s code=%s",
            state!!.name,
            reason!!.name,
            url,
            code
        )

        when (state) {
            CBLState.CODE_PAIR_RECEIVED -> try {
                val renderJSON = JSONObject()
                renderJSON.put(VERIFICATION_URI, url)
                renderJSON.put(USER_CODE, code)
                Timber.d(renderJSON.toString())
                alexaEngineManager.onReceiveCBLCode(url, code)
            } catch (e: Exception) {
                Timber.e(e.message)
            }
            CBLState.STARTING -> Timber.d("starting....")
            CBLState.STOPPING -> when (reason) {
                CBLStateChangedReason.CODE_PAIR_EXPIRED -> {
                    //showLoginButton()
                    try {
                        val renderJSON = JSONObject()
                        val expiredMessage =
                            "The code has expired. Retry to generate a new code."
                        renderJSON.put("message", expiredMessage)
                        Timber.w(renderJSON.toString())
                    } catch (e: JSONException) {
                        Timber.e(e.message)
                    }
                }
                CBLStateChangedReason.AUTHORIZATION_EXPIRED -> try {
                    val renderJSON = JSONObject()
                    val expiredMessage = "The token has expired. Log in again."
                    renderJSON.put("message", expiredMessage)
                    Timber.d(renderJSON.toString())
                } catch (e: JSONException) {
                    Timber.e(e.message)
                }
                //CBLStateChangedReason.NONE ->                         // CBL stopped using cancel button
                //showLoginButton()
                else -> {
                }
            }
            else -> {
            }
        }
    }

    override fun clearRefreshToken() {
        databaseManager.clearRefreshToken()
    }

    override fun setRefreshToken(refreshToken: String?) {
        this.refreshToken = refreshToken!!
        databaseManager.setRefreshToken(refreshToken)
    }

    override fun getRefreshToken(): String? {
        Timber.e("refreshToken %s", refreshToken)
        return refreshToken
    }

    fun startCBL() {
        Timber.d("startCBL")
        start()
    }
}