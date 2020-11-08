package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.alexa.AlexaClient
import com.jijith.alexa.service.interfaces.AuthStateObserver
import timber.log.Timber
import java.util.*

class AlexaClientHandler(private var context: Context) : AlexaClient() {

    // List of Authentication observers
    private var observers = HashSet<AuthStateObserver>()

    // Current AuthState and AuthError
    private var authState: AuthState? = null
    private var authError: AuthError? = null

    override fun authStateChanged(state: AuthState, error: AuthError) {
        super.authStateChanged(state, error)
        Timber.d("state= %s error= %s", state, error)

        notifyAuthStateObservers(state, error)
        authState = state
        authError = error
    }

    override fun connectionStatusChanged(
        status: ConnectionStatus?,
        reason: ConnectionChangedReason?
    ) {
        super.connectionStatusChanged(status, reason)
        Timber.d("status= %s reason= %s", status, reason)
    }

    override fun dialogStateChanged(state: DialogState?) {
        super.dialogStateChanged(state)
        Timber.d("state= %s ", state)
    }

    fun registerAuthStateObserver(observer: AuthStateObserver?) {
        synchronized(observers) {
            if (observer == null) return
            observers.add(observer)

            // notify newly registered observer with the current state
            observer.onAuthStateChanged(authState, authError)
        }
    }

    fun removeAuthStateObserver(observer: AuthStateObserver?) {
        synchronized(observers) {
            if (observer == null) return
            observers.remove(observer)
        }
    }

    private fun notifyAuthStateObservers(
        authState: AuthState,
        authError: AuthError
    ) {
        synchronized(observers) {
            for (observer in observers) {
                observer.onAuthStateChanged(authState, authError)
            }
        }
    }
}