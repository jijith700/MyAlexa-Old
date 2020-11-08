package com.jijith.alexa.service.handlers

import com.amazon.aace.alexa.Notifications
import timber.log.Timber

class NotificationsHandler : Notifications() {

    override fun setIndicator(state: IndicatorState?) {
        Timber.i("Indicator State: $state")
//        mActivity!!.runOnUiThread { mStateText!!.text = state?.toString() ?: "" }
    }
}