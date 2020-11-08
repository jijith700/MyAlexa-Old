package com.jijith.alexa.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.RemoteCallbackList
import androidx.core.app.NotificationCompat
import com.jijith.alexa.R
import com.jijith.alexa.lib.IMyAlexaCallbackInterface
import com.jijith.alexa.lib.IMyAlexaServiceInterface
import com.jijith.alexa.service.interfaces.managers.AlexaEngineManager
import com.jijith.alexa.service.managersimpl.AlexaEngineManagerImpl
import timber.log.Timber
import java.util.*

class AlexaService : Service() {

    // Binder given to clients
    private val binder = LocalBinder()

    // Random number generator
    private val mGenerator = Random()

    /** method for clients  */
    val randomNumber: Int
        get() = mGenerator.nextInt(100)

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): AlexaService = this@AlexaService
    }

    private lateinit var alexaEngineManager : AlexaEngineManager


    override fun onCreate() {
        super.onCreate()
//        alexaEngineManager = AlexaEngineManagerImpl(baseContext)
    }

    override fun onBind(intent: Intent): IBinder {
        return iMyAlexaServiceInterfaceBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runAsForeground()
        return START_STICKY
    }

    private fun runAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = "com.jijith.alexa"
            val channelName = "My Background Service"
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_NONE
            )
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            assert(manager != null)
            manager.createNotificationChannel(chan)
            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            val notification: Notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
            startForeground(11929129, notification)
        } else startForeground(1, Notification())
    }


    /**
     * instance of invalidation updating AIDL
     */
    private var iMyAlexaCallbackInterface: RemoteCallbackList<IMyAlexaCallbackInterface> =
        RemoteCallbackList<IMyAlexaCallbackInterface>()

    private var iMyAlexaServiceInterfaceBinder = object : IMyAlexaServiceInterface.Stub() {

            override fun registerCallback(iMyAlexaCallbackInterface: IMyAlexaCallbackInterface?) {
                this@AlexaService.iMyAlexaCallbackInterface.register(iMyAlexaCallbackInterface)
            }

        override fun startCBL() {
            Timber.d("startCBL")
            alexaEngineManager.startCBL()
        }

        override fun unregisterCallback(iMyAlexaCallbackInterface: IMyAlexaCallbackInterface?) {
                synchronized(this@AlexaService) {
                    this@AlexaService.iMyAlexaCallbackInterface.unregister(iMyAlexaCallbackInterface)
                }
            }
        }
}
