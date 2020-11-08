package com.jijith.alexa.service.handlers

import android.app.Activity
import android.content.Context
import android.widget.TextView
import com.amazon.aace.alexa.Alerts
import timber.log.Timber

class AlertsHandler(private var context: Context) : Alerts() {


    override fun alertStateChanged(
        alertToken: String?,
        state: AlertState?,
        reason: String?
    ) {
       Timber.i(
           String.format(
                "Alert State Changed. STATE: %s, REASON: %s, TOKEN: %s",
                state, reason, alertToken
            )
        )
//        mActivity!!.runOnUiThread { mStateText!!.text = state?.toString() ?: "" }
    }

    override fun alertCreated(alertToken: String?, detailedInfo: String?) {
       Timber.i(
           String.format(
                "Alert Created. TOKEN: %s, Detailed Info payload: %s",
                alertToken,
                detailedInfo
            )
        )
    }

    override fun alertDeleted(alertToken: String?) {
       Timber.i(String.format("Alert Deleted. TOKEN: %s", alertToken))
    }

    private fun onLocalStop() {
       Timber.i("Stopping active alert")
        super.localStop()
    }

    private fun onRemoveAllAlerts() {
       Timber.i("Removing all pending alerts from storage")
        super.removeAllAlerts()
    }
}