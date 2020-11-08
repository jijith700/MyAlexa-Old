package com.jijith.alexa.base

import android.app.Application
import android.content.Intent
import com.jijith.alexa.BuildConfig
import timber.log.Timber

class MyAlexa : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}