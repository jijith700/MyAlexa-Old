package com.jijith.alexa.hmi

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.jijith.alexa.R
import com.jijith.alexa.lib.IMyAlexaServiceInterface
import com.jijith.alexa.vo.User
import timber.log.Timber


class MainRepository(var context: Context) {

    private lateinit var iMyAlexaServiceInterface: IMyAlexaServiceInterface

    var loading = MutableLiveData<Int>()
    var success = MutableLiveData<Boolean>()
    var errrorMessage = MutableLiveData<String>()
    var user = MutableLiveData<User>()

    lateinit var serviceConnection : ServiceConnection


    /**
     * Variable hold the object of ApiService and the it will initialized here.
     */
    init {
        startService()
        connectService(IMyAlexaServiceInterface::class.java.name)
    }

    /**
     * Start the alexaService
     *
     */
    private fun startService() {
        val className = "com.jijith.alexa.service.AlexaService"
        val packageName = "com.jijith.alexa"
        val i = Intent(className)
        i.component = ComponentName(packageName, className)
            context.startService(i)
    }

    /**
     * Bind alexaService
     *
     * @param serviceName AIDL service name
     */
    private fun connectService(serviceName: String) {
        Timber.d(serviceName)
        val className = "com.jijith.alexa.service.AlexaService"
        val packageName = "com.jijith.alexa"
        serviceConnection = RemoteServiceConnection(context, serviceName)
        val i = Intent(className)
        i.action = serviceName
        i.component = ComponentName(packageName, className)
        val ret: Boolean =
            context.bindService(i, serviceConnection, Context.BIND_AUTO_CREATE)
        Toast.makeText(context, "BindService  $ret", Toast.LENGTH_LONG).show()
    }


    fun stopBinding() {
        context.unbindService(serviceConnection)
    }

    /**
     * Class makes the connection with alexa service
     */
    inner class RemoteServiceConnection internal constructor(
        var context: Context,
        var serviceName: String
    ) :
        ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName,
            boundService: IBinder
        ) {
            if (serviceName == IMyAlexaServiceInterface::class.java.name) {
                iMyAlexaServiceInterface = IMyAlexaServiceInterface.Stub.asInterface(boundService)
            }
            Toast
                .makeText(
                    context,
                    context.getString(R.string.service_connected),
                    Toast.LENGTH_LONG
                )
                .show()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Toast.makeText(
                    context, context.getString(R.string.service_disconnected),
                    Toast.LENGTH_LONG
                )
                .show()
        }
    }

    fun startCBL() {
        if (iMyAlexaServiceInterface != null) {
            iMyAlexaServiceInterface.startCBL()
        } else {
            Timber.d("error")
        }
    }
}