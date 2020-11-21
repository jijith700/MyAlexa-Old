package com.jijith.alexa.service.interfaces.managers

interface AlexaEngineManager {

    fun startCBL()

    fun onReceiveCBLCode(url: String?, code: String?)

}